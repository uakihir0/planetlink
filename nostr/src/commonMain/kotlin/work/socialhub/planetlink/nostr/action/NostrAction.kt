package work.socialhub.planetlink.nostr.action

import kotlin.time.Instant
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withLock
import work.socialhub.knostr.EventKind
import work.socialhub.knostr.NostrException
import work.socialhub.knostr.entity.Nip19Entity
import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.entity.NostrFilter
import work.socialhub.knostr.entity.NostrProfile
import work.socialhub.knostr.entity.UnsignedEvent
import work.socialhub.knostr.social.model.NostrDirectMessage
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.social.model.NostrChannel as KnostrChannel
import work.socialhub.knostr.social.model.NostrUser as KnostrUser
import work.socialhub.knostr.social.stream.NotificationStream
import work.socialhub.knostr.social.stream.TimelineStream
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.MentionCommentCallback
import work.socialhub.planetlink.action.callback.comment.NotificationCommentCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.StreamActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.utils.ExceptionHandler
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.model.event.NotificationEvent
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.ProfileForm
import work.socialhub.planetlink.nostr.model.NostrComment
import work.socialhub.planetlink.nostr.model.NostrPaging
import work.socialhub.planetlink.nostr.model.NostrUser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.js.JsExport

/** Action implementation for the Nostr platform */
@JsExport
class NostrAction(
    account: Account,
    val auth: NostrAuth,
) : AccountActionImpl(account) {

    companion object {
        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.FollowUser,
                SocialActionType.UnfollowUser,
                SocialActionType.MuteUser,
                SocialActionType.UnmuteUser,
                SocialActionType.GetRelationship,
                SocialActionType.GetComment,
                SocialActionType.GetContext,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.LikeComment,
                SocialActionType.UnlikeComment,
                SocialActionType.ShareComment,
                SocialActionType.ReactionComment,
                SocialActionType.UnreactionComment,
                SocialActionType.GetNotification,
                SocialActionType.UpdateProfile,
                SocialActionType.GetUserBookmarks,
                SocialActionType.BookmarkComment,
                SocialActionType.UnbookmarkComment,
                SocialActionType.VotePoll,
                SocialActionType.GetChannels,
                SocialActionType.CreateList,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.MentionTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.UserLikeTimeLine,
                TimeLineActionType.UserMediaTimeLine,
                TimeLineActionType.SearchTimeLine,
                TimeLineActionType.MessageTimeLine,
                TimeLineActionType.UserBookmarkTimeLine,
                TimeLineActionType.ChannelTimeLine,

                UsersActionType.GetFollowingUsers,
                UsersActionType.GetFollowerUsers,
                UsersActionType.SearchUsers,

                MessageActionType.GetMessageThread,
                MessageActionType.GetMessageTimeLine,
                MessageActionType.PostMessage,

                StreamActionType.HomeTimeLineStream,
                StreamActionType.NotificationStream,
                StreamActionType.CommentUpdateStream,
            )
        )

        /** 通知の対象投稿をリレーから取得する際の同時リクエスト数 */
        private const val TARGET_COMMENT_FETCH_CHUNK_SIZE = 8

        /**
         * 通知が対象としている投稿のイベント ID を取得
         *
         * メンション (kind:1) は通知イベント自身が対象の投稿となる.
         * リポスト (NIP-18) / リアクション (NIP-25) / Zap (NIP-57) は
         * 対象の投稿を e タグで参照する.
         * (いずれも規約上「最後の e タグ」が対象となる)
         */
        internal fun targetEventId(event: NostrEvent): String? {
            if (event.kind == EventKind.TEXT_NOTE) {
                return event.id
            }
            lastEventTag(event.tags)?.let { return it }

            // Zap レシートは e タグを持たない場合があるため,
            // 内包されている Zap リクエスト (kind:9734) からも参照する.
            if (event.kind == EventKind.ZAP_RECEIPT) {
                return extractZapRequestEventId(event.tags)
            }
            return null
        }

        private fun lastEventTag(tags: List<List<String>>): String? {
            return tags
                .lastOrNull { it.size >= 2 && it[0] == "e" }
                ?.get(1)
        }

        private fun extractZapRequestEventId(tags: List<List<String>>): String? {
            val descriptionTag = tags
                .firstOrNull { it.size >= 2 && it[0] == "description" }
                ?: return null
            return try {
                val request = Json.parseToJsonElement(descriptionTag[1]).jsonObject
                if (request["kind"]?.jsonPrimitive?.content != "9734") {
                    return null
                }
                val requestTags = request["tags"]?.jsonArray
                    ?.map { tag -> tag.jsonArray.map { it.jsonPrimitive.content } }
                    ?: return null
                lastEventTag(requestTags)
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun capabilities(): Capabilities = CAPABILITIES

    private val accessor get() = auth.accessor
    private val social get() = accessor.social
    private val nostr get() = accessor.nostr
    private val pubkey get() = accessor.pubkey
    private var relayConnected = false
    private val relayMutex = kotlinx.coroutines.sync.Mutex()
    private val enrichmentDispatcher by lazy {
        NostrEnrichmentDispatcher(social.enrichment())
    }

    private suspend fun ensureRelayConnected() {
        if (relayConnected) return
        relayMutex.withLock {
            if (relayConnected) return
            val scope = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()
            )
            val config = nostr.config()
            for (url in config.relayUrls) {
                nostr.relayPool().addRelay(url, config)
            }
            nostr.relayPool().connectAll(scope)

            val totalRelays = config.relayUrls.size
            repeat(25) {
                val connected = nostr.relays().getConnectedRelays().size
                if (connected >= totalRelays) {
                    relayConnected = true
                    return
                }
                if (connected > 0 && it >= 10) {
                    relayConnected = true
                    return
                }
                kotlinx.coroutines.delay(200)
            }
            if (nostr.relays().getConnectedRelays().isNotEmpty()) {
                relayConnected = true
                return
            }
            throw SocialHubException("Failed to connect to any Nostr relay within 5 seconds")
        }
    }

    // ============================================================== //
    // Account API
    // ============================================================== //

    override suspend fun userMe(): User {
        ensureRelayConnected()
        return proceed {
            val response = social.users().getProfile(pubkey)
            val user = NostrMapper.user(response.data, service())
            me = user
            user
        }
    }

    private suspend fun fetchUserMe(): User {
        ensureRelayConnected()
        return proceed {
            val response = social.users().getProfile(pubkey)
            val user = NostrMapper.user(response.data, service())
            me = user
            user
        }
    }

    override suspend fun userMeWithCache(): User {
        return me ?: fetchUserMe()
    }

    override suspend fun user(id: Identify): User {
        ensureRelayConnected()
        val key = id.id!!.value<String>()
        if (key == pubkey && me != null) return me!!

        return proceed {
            val response = social.users().getProfile(key)
            NostrMapper.user(response.data, service())
        }
    }

    override suspend fun user(url: String): User {
        ensureRelayConnected()
        return proceed {
            val trimmed = url.trim()
            when {
                trimmed.startsWith("npub1") -> {
                    val entity = nostr.nip().decodeNip19(trimmed)
                    val pubkey = when (entity) {
                        is Nip19Entity.NPub -> entity.pubkey
                        is Nip19Entity.NProfile -> entity.pubkey
                        else -> throw SocialHubException("Invalid nostr user URL: $url")
                    }
                    val response = social.users().getProfile(pubkey)
                    NostrMapper.user(response.data, service())
                }
                trimmed.startsWith("nprofile1") -> {
                    val entity = nostr.nip().decodeNip19(trimmed)
                    val pubkey = (entity as? Nip19Entity.NProfile)?.pubkey
                        ?: throw SocialHubException("Invalid nprofile URL: $url")
                    val response = social.users().getProfile(pubkey)
                    NostrMapper.user(response.data, service())
                }
                trimmed.length == 64 && trimmed.all { it in '0'..'9' || it in 'a'..'f' } -> {
                    val response = social.users().getProfile(trimmed)
                    NostrMapper.user(response.data, service())
                }
                trimmed.contains("@") -> {
                    val result = nostr.nip().resolveNip05(trimmed)
                    val names = result.data.names
                    if (names.isEmpty()) throw SocialHubException("NIP-05 resolution failed: $url")
                    val pubkey = names.entries.first().value
                    val response = social.users().getProfile(pubkey)
                    NostrMapper.user(response.data, service())
                }
                else -> throw SocialHubException("Invalid nostr user URL: $url")
            }
        }
    }

    override suspend fun followUser(id: Identify) {
        proceedUnit {
            social.users().follow(id.id!!.value<String>())
        }
    }

    override suspend fun unfollowUser(id: Identify) {
        proceedUnit {
            social.users().unfollow(id.id!!.value<String>())
        }
    }

    override suspend fun muteUser(id: Identify) {
        proceedUnit {
            social.mutes().mute(id.id!!.value<String>())
        }
    }

    override suspend fun unmuteUser(id: Identify) {
        proceedUnit {
            social.mutes().unmute(id.id!!.value<String>())
        }
    }

    /**
     * {@inheritDoc}
     * kind:0 は全置換のため、既存プロフィールを取得してマージする。
     * avatar/banner は設定済みの NIP-96 サーバーにアップロードして URL を設定。
     */
    override suspend fun updateProfile(form: ProfileForm) {
        ensureRelayConnected()
        proceedUnit {
            val existing = social.users().getProfile(pubkey).data

            var picture = existing.picture
            var banner = existing.banner

            form.avatar?.let { bytes ->
                val uploaded = social.media().uploadToConfiguredServer(
                    fileData = bytes,
                    fileName = form.avatarName ?: "avatar",
                    mimeType = NostrMapper.nostrMediaMimeType(form.avatarName),
                )
                if (uploaded.data.url.isNotEmpty()) picture = uploaded.data.url
            }
            form.banner?.let { bytes ->
                val uploaded = social.media().uploadToConfiguredServer(
                    fileData = bytes,
                    fileName = form.bannerName ?: "banner",
                    mimeType = NostrMapper.nostrMediaMimeType(form.bannerName),
                )
                if (uploaded.data.url.isNotEmpty()) banner = uploaded.data.url
            }

            val profile = NostrProfile(
                name = existing.name,
                about = form.description ?: existing.about,
                picture = picture,
                banner = banner,
                nip05 = existing.nip05,
                displayName = form.displayName ?: existing.displayName,
                website = existing.website,
                lud16 = existing.lud16,
            )
            social.users().updateProfile(profile)
        }
    }

    override suspend fun blockUser(id: Identify) {
        throw NotSupportedException("Nostr does not support blocking users")
    }

    override suspend fun unblockUser(id: Identify) {
        throw NotSupportedException("Nostr does not support blocking users")
    }

    override suspend fun relationship(id: Identify): Relationship {
        return proceed {
            val response = social.users().getRelationship(id.id!!.value<String>())
            NostrMapper.relationship(response.data)
        }
    }

    // ============================================================== //
    // User API
    // ============================================================== //

    override suspend fun followingUsers(id: Identify, paging: Paging): Pageable<User> {
        return proceed {
            val response = social.users().getFollowing(id.id!!.value<String>())
            val pubkeys = response.data
            if (pubkeys.isEmpty()) {
                return@proceed Pageable<User>().also { it.paging = paging }
            }
            val profiles = social.users().getProfiles(pubkeys)
            NostrMapper.usersToPageable(profiles.data, service(), paging)
        }
    }

    override suspend fun followerUsers(id: Identify, paging: Paging): Pageable<User> {
        return proceed {
            val response = social.users().getFollowersWithProfiles(id.id!!.value<String>())
            NostrMapper.usersToPageable(response.data, service(), paging)
        }
    }

    override suspend fun searchUsers(query: String, paging: Paging): Pageable<User> {
        return proceed {
            val response = social.search().searchUsers(query)
            NostrMapper.usersToPageable(response.data, service(), paging)
        }
    }

    // ============================================================== //
    // TimeLine API
    // ============================================================== //

    override suspend fun homeTimeLine(paging: Paging): Pageable<Comment> {
        ensureRelayConnected()
        return proceed {
            val np = NostrPaging.fromPaging(paging)
            val response = social.feed().getHomeFeed(
                until = np.until,
                limit = paging.count ?: 50,
            )
            val userMe = userMeWithCache()
            NostrMapper.timeLine(response.data, service(), paging, userMe)
        }
    }

    override suspend fun mentionTimeLine(paging: Paging): Pageable<Comment> {
        ensureRelayConnected()
        return proceed {
            val np = NostrPaging.fromPaging(paging)
            val response = social.feed().getMentions(
                until = np.until,
                limit = paging.count ?: 50,
            )
            val userMe = userMeWithCache()
            NostrMapper.timeLine(response.data, service(), paging, userMe)
        }
    }

    override suspend fun notification(paging: Paging): Pageable<Notification> {
        ensureRelayConnected()
        return proceed {
            val np = NostrPaging.fromPaging(paging)

            val filter = NostrFilter(
                pTags = listOf(pubkey),
                kinds = listOf(
                    EventKind.TEXT_NOTE,
                    EventKind.REPOST,
                    EventKind.REACTION,
                    EventKind.ZAP_RECEIPT,
                ),
                until = np.until,
                limit = paging.count ?: 50,
            )
            val response = nostr.events().queryEvents(listOf(filter))

            val events = response.data.filter { it.pubkey != pubkey }

            val senderPubkeys = events.map { event ->
                if (event.kind == EventKind.ZAP_RECEIPT) {
                    extractZapSenderPubkey(event.tags) ?: event.pubkey
                } else {
                    event.pubkey
                }
            }.distinct().filter { it != pubkey }

            val profileMap = if (senderPubkeys.isNotEmpty()) {
                try {
                    social.users().getProfiles(senderPubkeys).data
                        .associateBy { it.pubkey }
                } catch (_: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            // 通知の対象となった投稿を解決
            // (リポスト/リアクション/Zap は対象イベントを e タグから参照する)
            val commentMap = targetComments(
                events.mapNotNull { targetEventId(it) }.distinct()
            )

            val notifications = events.map { event ->
                val senderPubkey = if (event.kind == EventKind.ZAP_RECEIPT) {
                    extractZapSenderPubkey(event.tags) ?: event.pubkey
                } else {
                    event.pubkey
                }

                Notification(service()).apply {
                    id = ID(event.id)
                    createAt = Instant.fromEpochSeconds(event.createdAt, 0)

                    when (event.kind) {
                        EventKind.TEXT_NOTE -> {
                            action = NotificationActionType.MENTION.code
                            type = "mention"
                        }
                        EventKind.REPOST -> {
                            action = NotificationActionType.SHARE.code
                            type = "repost"
                        }
                        EventKind.ZAP_RECEIPT -> {
                            action = NotificationActionType.LIKE.code
                            type = "zap"
                        }
                        else -> {
                            action = NotificationActionType.LIKE.code
                            type = "reaction"
                        }
                    }

                    // ステータス情報
                    // (リレーから取得できない場合はメンション本文のみを設定)
                    val target = targetEventId(event)?.let { commentMap[it] }
                    comments = when {
                        target != null -> listOf(target)
                        event.kind == EventKind.TEXT_NOTE -> listOf(
                            NostrComment(service()).apply {
                                id = ID(event.id)
                                createAt = Instant.fromEpochSeconds(event.createdAt, 0)
                                text = work.socialhub.planetlink.model.common.AttributedString
                                    .plain(event.content)
                            }
                        )
                        else -> null
                    }

                    val profile = profileMap[senderPubkey]
                    users = listOf(
                        if (profile != null) {
                            NostrMapper.user(profile, service())
                        } else {
                            User(service()).apply {
                                id = ID(senderPubkey)
                                name = senderPubkey.take(8)
                            }
                        }
                    )
                }
            }.sortedByDescending { it.createAt }

            Pageable<Notification>().also { p ->
                p.entities = notifications
                p.paging = NostrPaging.fromPaging(paging)
            }
        }
    }

    /**
     * 対象の投稿をイベント ID から解決
     *
     * 解決できなかった投稿は結果に含まれない.
     * (削除済みや保持しているリレーに存在しない投稿があり得るため)
     */
    private suspend fun targetComments(
        eventIds: List<String>
    ): Map<String, Comment> {
        if (eventIds.isEmpty()) {
            return emptyMap()
        }

        val userMe = try {
            me ?: fetchUserMe()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        val comments = mutableMapOf<String, Comment>()

        // リレーへの同時リクエスト数を抑えるために分割して取得
        // (取得済みの投稿は knostr のキャッシュから返るためリクエストは発生しない)
        for (chunk in eventIds.chunked(TARGET_COMMENT_FETCH_CHUNK_SIZE)) {
            val results = coroutineScope {
                chunk.map { eventId ->
                    async {
                        try {
                            val response = social.feed().getNote(eventId)
                            eventId to NostrMapper.comment(response.data, service(), userMe)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll()
            }
            results.filterNotNull().forEach { (eventId, comment) ->
                comments[eventId] = comment
            }
        }
        return comments
    }

    private fun extractZapSenderPubkey(tags: List<List<String>>): String? {
        val descriptionTag = tags.firstOrNull { it.size >= 2 && it[0] == "description" }
            ?: return null
        return try {
            val json = Json.parseToJsonElement(descriptionTag[1])
            json.jsonObject["pubkey"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun userCommentTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        ensureRelayConnected()
        return proceed {
            val np = NostrPaging.fromPaging(paging)
            val response = social.feed().getUserFeed(
                pubkey = id.id!!.value<String>(),
                until = np.until,
                limit = paging.count ?: 50,
            )
            val userMe = userMeWithCache()
            NostrMapper.timeLine(response.data, service(), paging, userMe)
        }
    }

    override suspend fun userLikeTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        ensureRelayConnected()
        return proceed {
            val np = NostrPaging.fromPaging(paging)
            val response = social.feed().getUserLikesFeed(
                pubkey = id.id!!.value<String>(),
                until = np.until,
                limit = paging.count ?: 50,
            )
            val userMe = userMeWithCache()
            NostrMapper.timeLine(response.data, service(), paging, userMe)
        }
    }

    override suspend fun userMediaTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        ensureRelayConnected()
        return proceed {
            val np = NostrPaging.fromPaging(paging)
            val response = social.feed().getUserMediaFeed(
                pubkey = id.id!!.value<String>(),
                until = np.until,
                limit = paging.count ?: 50,
            )
            val userMe = userMeWithCache()
            NostrMapper.timeLine(response.data, service(), paging, userMe)
        }
    }

    override suspend fun searchTimeLine(query: String, paging: Paging): Pageable<Comment> {
        ensureRelayConnected()
        return proceed {
            val response = social.search().searchNotes(query)
            val userMe = userMeWithCache()
            NostrMapper.timeLine(response.data, service(), paging, userMe)
        }
    }

    override suspend fun userBookmarkTimeLine(paging: Paging): Pageable<Comment> {
        return fetchUserBookmarks(paging)
    }

    private suspend fun fetchUserBookmarks(paging: Paging): Pageable<Comment> {
        return proceed {
            ensureRelayConnected()
            val eventIds = social.bookmarks().getBookmarks().data
            if (eventIds.isEmpty()) {
                return@proceed Pageable<Comment>().also { it.paging = NostrPaging.fromPaging(paging) }
            }

            val pageSize = paging.count ?: 50
            val batchSize = pageSize * 3
            val np = NostrPaging.fromPaging(paging)
            val allIds = eventIds.asReversed()

            val notes = mutableListOf<NostrNote>()
            for (offset in 0 until allIds.size step batchSize) {
                val batchIds = allIds.drop(offset).take(batchSize)
                if (batchIds.isEmpty()) break

                val batchNotes: List<NostrNote> = coroutineScope {
                    batchIds.map { eventId ->
                        async<NostrNote?> {
                            try {
                                social.feed().getNote(eventId).data
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: NostrException) {
                                if (e.message == "Note not found: $eventId") null else throw e
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                notes.addAll(batchNotes)

                val inWindow = notes.count {
                    (np.since == null || it.createdAt >= np.since!!) &&
                        (np.until == null || it.createdAt <= np.until!!)
                }
                if (inWindow >= pageSize) break
            }

            val filtered = notes
                .filter { np.since == null || it.createdAt >= np.since!! }
                .filter { np.until == null || it.createdAt <= np.until!! }
                .sortedByDescending { it.createdAt }
                .take(pageSize)
            NostrMapper.timeLine(filtered, service(), paging, me ?: fetchUserMe())
        }
    }

    // ============================================================== //
    // Comment API
    // ============================================================== //

    override suspend fun postComment(req: CommentForm) {
        doPostComment(req)
    }

    private suspend fun doPostComment(req: CommentForm) {
        proceedUnit {
            ensureRelayConnected()

            if (req.isMessage) {
                doSendDirectMessage(req)
                return@proceedUnit
            }

            val channelId = req.params[NostrComment.CHANNEL_ID_KEY] as? String
            if (channelId != null) {
                if (req.poll != null) {
                    throw NotSupportedException("Polls are not supported in Nostr channels")
                }
                val replyToEventId = req.replyId?.value<String>()
                if (replyToEventId != null) {
                    val signer = nostr.signer()
                        ?: throw SocialHubException("Signer is required for channel reply")
                    val unsigned = UnsignedEvent(
                        pubkey = signer.getPublicKey(),
                        createdAt = Clock.System.now().epochSeconds,
                        kind = EventKind.CHANNEL_MESSAGE,
                        tags = listOf(
                            listOf("e", channelId, "", "root"),
                            listOf("e", replyToEventId, "", "reply"),
                        ),
                        content = contentWithUploadedMedia(req),
                    )
                    val signed = signer.sign(unsigned)
                    nostr.events().publishEvent(signed)
                } else {
                    social.channels().sendMessage(channelId, contentWithUploadedMedia(req))
                }
                return@proceedUnit
            }

            req.poll?.let { poll ->
                if (req.images.isNotEmpty() || req.replyId != null || req.quoteId != null) {
                    throw NotSupportedException("Nostr polls cannot include media, replies, or quotes")
                }
                if (poll.multiple) {
                    throw NotSupportedException("Nostr does not support multiple-choice polls")
                }
                if (poll.options.size < 2) {
                    throw SocialHubException("Nostr polls require at least two options")
                }
                val closedAt = poll.expiresIn
                    .takeIf { it > 0 }
                    ?.let { Clock.System.now().epochSeconds + it * 60L }
                social.polls().createPoll(
                    content = req.text.orEmpty(),
                    options = poll.options,
                    closedAt = closedAt,
                )
                return@proceedUnit
            }

            val replyToEventId = req.replyId?.value<String>()
            val quoteEventId = req.quoteId?.value<String>()
            val quoteTags = quoteEventId?.let { listOf(listOf("q", it)) }.orEmpty()
            val uploads = req.images.map { NostrMapper.toNostrMediaUpload(it) }

            if (uploads.isNotEmpty() && replyToEventId != null) {
                social.media().uploadAndReply(
                    uploads = uploads,
                    replyToEventId = replyToEventId,
                    content = req.text.orEmpty(),
                    tags = quoteTags,
                    contentWarning = req.warning,
                    sensitive = req.isSensitive,
                )
            } else if (uploads.isNotEmpty()) {
                social.media().uploadManyAndPost(
                    uploads = uploads,
                    content = req.text.orEmpty(),
                    tags = quoteTags,
                    contentWarning = req.warning,
                    sensitive = req.isSensitive,
                )
            } else if (replyToEventId != null) {
                social.feed().reply(
                    content = req.text ?: "",
                    tags = quoteTags,
                    replyToEventId = replyToEventId,
                    contentWarning = req.warning,
                    sensitive = req.isSensitive,
                )
            } else if (quoteEventId != null) {
                social.feed().quoteRepost(
                    eventId = quoteEventId,
                    comment = req.text.orEmpty(),
                    contentWarning = req.warning,
                    sensitive = req.isSensitive,
                )
            } else {
                social.feed().post(
                    content = req.text ?: "",
                    contentWarning = req.warning,
                    sensitive = req.isSensitive,
                )
            }
        }
    }

    override suspend fun comment(id: Identify): Comment {
        ensureRelayConnected()
        return proceed {
            val eventId = id.id!!.value<String>()
            val response = social.feed().getNote(eventId)
            val userMe = userMeWithCache()
            NostrMapper.comment(response.data, service(), userMe)
        }
    }

    override suspend fun comment(url: String): Comment {
        ensureRelayConnected()
        return proceed {
            val trimmed = url.trim()
            val eventId = when {
                trimmed.startsWith("note1") -> {
                    val entity = nostr.nip().decodeNip19(trimmed)
                    (entity as? Nip19Entity.Note)?.eventId
                        ?: throw SocialHubException("Invalid note URL: $url")
                }
                trimmed.startsWith("nevent1") -> {
                    val entity = nostr.nip().decodeNip19(trimmed)
                    (entity as? Nip19Entity.NEvent)?.eventId
                        ?: throw SocialHubException("Invalid nevent URL: $url")
                }
                trimmed.length == 64 && trimmed.all { it in '0'..'9' || it in 'a'..'f' } -> {
                    trimmed
                }
                else -> throw SocialHubException("Invalid nostr comment URL: $url")
            }
            val response = social.feed().getNote(eventId)
            val userMe = userMeWithCache()
            NostrMapper.comment(response.data, service(), userMe)
        }
    }

    override suspend fun likeComment(id: Identify) {
        val eventId = id.id!!.value<String>()
        val authorPubkey = getAuthorPubkey(id)
        proceedUnit {
            social.reactions().like(eventId, authorPubkey)
        }
    }

    override suspend fun unlikeComment(id: Identify) {
        val eventId = id.id!!.value<String>()
        proceedUnit {
            social.reactions().unlike(eventId)
        }
    }

    override suspend fun shareComment(id: Identify) {
        val eventId = id.id!!.value<String>()
        proceedUnit {
            social.feed().repost(eventId)
        }
    }

    override suspend fun unshareComment(id: Identify) {
        throw NotSupportedException("Unshare is not yet supported for Nostr")
    }

    override suspend fun reactionComment(id: Identify, reaction: String) {
        val eventId = id.id!!.value<String>()
        val authorPubkey = getAuthorPubkey(id)
        proceedUnit {
            social.reactions().react(eventId, authorPubkey, reaction)
        }
    }

    override suspend fun unreactionComment(id: Identify, reaction: String) {
        val eventId = id.id!!.value<String>()
        proceedUnit {
            social.reactions().unreact(eventId, reaction)
        }
    }

    override suspend fun deleteComment(id: Identify) {
        val eventId = id.id!!.value<String>()
        proceedUnit {
            social.feed().delete(eventId)
        }
    }

    override suspend fun bookmarkComment(id: Identify) {
        proceedUnit {
            ensureRelayConnected()
            social.bookmarks().bookmark(id.id!!.value<String>())
        }
    }

    override suspend fun unbookmarkComment(id: Identify) {
        proceedUnit {
            ensureRelayConnected()
            social.bookmarks().unbookmark(id.id!!.value<String>())
        }
    }

    override suspend fun votePoll(id: Identify, choices: List<Int>) {
        if (choices.isEmpty()) {
            throw SocialHubException("At least one poll choice is required")
        }
        proceedUnit {
            ensureRelayConnected()
            social.polls().vote(id.id!!.value<String>(), choices)
        }
    }

    override suspend fun commentContexts(id: Identify): Context {
        return proceed {
            val eventId = id.id!!.value<String>()
            val response = social.feed().getThread(eventId)
            val userMe = userMeWithCache()

            NostrMapper.commentContext(response.data, service(), userMe)
        }
    }

    // ============================================================== //
    // Channel API
    // ============================================================== //

    override suspend fun channels(id: Identify, paging: Paging): Pageable<Channel> {
        return proceed {
            ensureRelayConnected()
            val np = NostrPaging.fromPaging(paging)
            val filter = NostrFilter(
                kinds = listOf(EventKind.CHANNEL_CREATE),
                since = np.since,
                until = np.until,
                limit = paging.count ?: 50,
            )
            val response = nostr.events().queryEvents(listOf(filter))
            val channels = response.data.map { event ->
                KnostrChannel().apply {
                    this.id = event.id
                    this.createdAt = event.createdAt
                    try {
                        val meta = Json.parseToJsonElement(event.content).jsonObject
                        this.name = meta["name"]?.jsonPrimitive?.content.orEmpty()
                        this.about = meta["about"]?.jsonPrimitive?.content.orEmpty()
                        this.picture = meta["picture"]?.jsonPrimitive?.content.orEmpty()
                    } catch (_: Exception) {}
                }
            }
            NostrMapper.channels(channels, service(), paging)
        }
    }

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return proceed {
            ensureRelayConnected()
            val np = NostrPaging.fromPaging(paging)
            val messages = social.channels().getChannelMessages(
                channelId = id.id!!.value<String>(),
                since = np.since,
                until = np.until,
                limit = paging.count ?: 50,
            ).data
            val pubkeys = messages.map { it.event.pubkey }.distinct()
            val users = if (pubkeys.isEmpty()) {
                emptyMap()
            } else {
                try {
                    social.users().getProfiles(pubkeys).data.associateBy { it.pubkey }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyMap()
                }
            }
            NostrMapper.channelMessages(messages, users, service(), paging)
        }
    }

    override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException("NIP-28 does not provide a channel member list")
    }

    override suspend fun createList(name: String, description: String?): Channel {
        return proceed {
            ensureRelayConnected()
            val event = social.channels().createChannel(
                name = name,
                about = description.orEmpty(),
            ).data
            Channel(service()).apply {
                id = ID(event.id)
                this.name = name
                this.description = description
                createAt = Instant.fromEpochSeconds(event.createdAt)
                isPublic = true
            }
        }
    }

    // ============================================================== //
    // Message API
    // ============================================================== //

    override suspend fun messageThread(paging: Paging): Pageable<Thread> {
        return proceed {
            val response = social.messages().getThreads()
            val threads = response.data.mapNotNull { dmThread ->
                val threadId = dmThread.rootNote?.event?.pubkey
                    ?: dmThread.replies.firstOrNull()?.event?.pubkey
                    ?: return@mapNotNull null
                Thread(service()).apply {
                    id = ID(threadId)
                    lastUpdate = dmThread.rootNote?.let {
                        Instant.fromEpochSeconds(it.createdAt, 0)
                    }
                }
            }
            Pageable<Thread>().also {
                it.entities = threads
                it.paging = paging
            }
        }
    }

    override suspend fun messageTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val pubkey = id.id!!.value<String>()
            val response = social.messages().getConversation(pubkey)
            val userMe = userMeWithCache()

            val comments = response.data.map { dm ->
                NostrComment(service()).apply {
                    this.id = ID(dm.id)
                    this.eventId = dm.id
                    createAt = Instant.fromEpochSeconds(dm.createdAt, 0)
                    text = work.socialhub.planetlink.model.common.AttributedString.plain(dm.content)
                    directMessage = true

                    val authorPubkey = dm.senderPubkey
                    if (authorPubkey == this@NostrAction.pubkey) {
                        this.user = userMe
                    } else {
                        try {
                            val profile = social.users().getProfile(authorPubkey)
                            this.user = NostrMapper.user(profile.data, service())
                        } catch (e: Exception) {
                            // Failed to load profile for $authorPubkey, use fallback
                            this.user = NostrUser(service()).apply {
                                this.id = ID(authorPubkey)
                                name = authorPubkey.take(8)
                            }
                        }
                    }
                }
            }

            Pageable<Comment>().also {
                it.entities = comments
                it.paging = paging
            }
        }
    }

    override suspend fun postMessage(req: CommentForm) {
        proceedUnit {
            ensureRelayConnected()
            doSendDirectMessage(req)
        }
    }

    private suspend fun doSendDirectMessage(req: CommentForm) {
        val recipientPubkey = req.replyId?.value<String>()
            ?: throw SocialHubException("recipient pubkey is required for direct message")
        social.messages().sendMessage(recipientPubkey, contentWithUploadedMedia(req))
    }

    private suspend fun contentWithUploadedMedia(req: CommentForm): String {
        val urls: List<String> = coroutineScope {
            req.images.map { image ->
                async {
                    val media = social.media().uploadToConfiguredServer(
                        fileData = image.data,
                        fileName = image.name,
                        mimeType = NostrMapper.nostrMediaMimeType(image.name),
                        description = image.description.orEmpty(),
                    ).data
                    media.url.takeIf { it.isNotBlank() }
                        ?: throw SocialHubException("NIP-96 upload returned an empty media URL")
                }
            }.awaitAll()
        }
        return buildList {
            req.text?.takeIf { it.isNotBlank() }?.let(::add)
            addAll(urls.filter { it.isNotBlank() })
        }.joinToString("\n")
    }

    // ============================================================== //
    // Stream
    // ============================================================== //

    override suspend fun setHomeTimeLineStream(callback: EventCallback): Stream {
        val userMe = userMeWithCache()
        val cache = social.cache()
        val stream = NostrStream(
            accessor = accessor,
            timelineStream = TimelineStream(
                nostr,
                cache,
                social.enrichment(),
            ).also { ts ->
                ts.onNoteCallback = { note ->
                    if (callback is UpdateCommentCallback) {
                        val comment = NostrMapper.comment(note, service(), userMe)
                        callback.onUpdate(CommentEvent(comment))
                    }
                }
            }
        )
        stream.open()
        return stream
    }

    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        val userMe = userMeWithCache()
        val cache = social.cache()
        val stream = NostrStream(
            accessor = accessor,
            notificationStream = NotificationStream(
                nostr,
                cache,
                social.enrichment(),
            ).also { ns ->
                ns.onMentionCallback = { note ->
                    if (callback is MentionCommentCallback) {
                        val comment = NostrMapper.comment(note, service(), userMe)
                        callback.onMention(CommentEvent(comment))
                    }
                }
                ns.onReactionCallback = { reaction ->
                    if (callback is NotificationCommentCallback) {
                        val notification = Notification(service()).apply {
                            id = ID(reaction.event.id)
                            action = NotificationActionType.LIKE.code
                            createAt = Instant.fromEpochSeconds(reaction.createdAt, 0)
                            reaction.author?.let { author ->
                                users = listOf(NostrMapper.user(author, service()))
                            }
                        }
                        callback.onNotification(NotificationEvent(notification))
                    }
                }
            }
        )
        stream.open()
        return stream
    }

    override suspend fun setCommentUpdateStream(
        comments: List<Comment>,
        callback: EventCallback,
    ): CommentUpdateStream {
        val stream = NostrCommentUpdateStream(
            enrichment = social.enrichment(),
            dispatcher = enrichmentDispatcher,
            callback = callback,
            service = service(),
            userMe = me ?: fetchUserMe(),
        )
        stream.addComments(comments)
        return stream
    }

    override fun request(): RequestAction {
        return NostrRequest(account)
    }

    // ============================================================== //
    // Internal
    // ============================================================== //

    private fun getAuthorPubkey(id: Identify): String {
        if (id is NostrComment) {
            return id.user?.id?.value<String>()
                ?: id.authorPubkey
                ?: pubkey
        }
        return pubkey
    }

    private fun service(): Service = account.service

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Nostr,
            runner = runner,
        )
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Nostr,
            runner = runner,
        )
    }
}
