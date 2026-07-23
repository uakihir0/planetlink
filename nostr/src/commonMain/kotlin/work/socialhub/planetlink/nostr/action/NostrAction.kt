package work.socialhub.planetlink.nostr.action

import kotlin.time.Instant
import kotlinx.coroutines.sync.withLock
import work.socialhub.knostr.EventKind
import work.socialhub.knostr.entity.Nip19Entity
import work.socialhub.knostr.entity.NostrFilter
import work.socialhub.knostr.entity.NostrProfile
import work.socialhub.knostr.social.model.NostrDirectMessage
import work.socialhub.knostr.social.model.NostrNote
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

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.MentionTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.UserLikeTimeLine,
                TimeLineActionType.UserMediaTimeLine,
                TimeLineActionType.SearchTimeLine,
                TimeLineActionType.MessageTimeLine,

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
     * avatar/banner は NIP-96 サーバー (auth.nip96Server) にアップロードして URL を設定。
     */
    override suspend fun updateProfile(form: ProfileForm) {
        ensureRelayConnected()
        proceedUnit {
            val existing = social.users().getProfile(pubkey).data

            var picture = existing.picture
            var banner = existing.banner

            form.avatar?.let { bytes ->
                val uploaded = social.media().upload(
                    serverUrl = auth.nip96Server,
                    fileData = bytes,
                    fileName = form.avatarName ?: "avatar",
                    mimeType = guessImageMimeType(form.avatarName),
                )
                if (uploaded.data.url.isNotEmpty()) picture = uploaded.data.url
            }
            form.banner?.let { bytes ->
                val uploaded = social.media().upload(
                    serverUrl = auth.nip96Server,
                    fileData = bytes,
                    fileName = form.bannerName ?: "banner",
                    mimeType = guessImageMimeType(form.bannerName),
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

    private fun guessImageMimeType(fileName: String?): String =
        when (fileName?.substringAfterLast('.', "")?.lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
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
                            comments = listOf(
                                NostrComment(service()).apply {
                                    id = ID(event.id)
                                    createAt = Instant.fromEpochSeconds(event.createdAt, 0)
                                    text = work.socialhub.planetlink.model.common.AttributedString.plain(event.content)
                                }
                            )
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

    // ============================================================== //
    // Comment API
    // ============================================================== //

    override suspend fun postComment(req: CommentForm) {
        proceedUnit {
            val replyToEventId = if (!req.isMessage) req.replyId?.value<String>() else null

            if (replyToEventId != null) {
                social.feed().reply(
                    content = req.text ?: "",
                    replyToEventId = replyToEventId,
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

    override suspend fun commentContexts(id: Identify): Context {
        return proceed {
            val eventId = id.id!!.value<String>()
            val response = social.feed().getThread(eventId)
            val userMe = userMeWithCache()

            val context = Context()
            context.ancestors = response.data.rootNote?.let { root ->
                listOf(NostrMapper.comment(root, service(), userMe))
            } ?: emptyList()

            context.descendants = response.data.replies.map { reply ->
                NostrMapper.comment(reply, service(), userMe)
            }

            context.sort()
            context
        }
    }

    // ============================================================== //
    // Channel API
    // ============================================================== //

    override suspend fun channels(id: Identify, paging: Paging): Pageable<Channel> {
        throw NotSupportedException("Nostr channels (NIP-28) are not yet supported")
    }

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Nostr channels (NIP-28) are not yet supported")
    }

    override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException("Nostr channels (NIP-28) are not yet supported")
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
            val recipientPubkey = req.replyId?.value<String>()
                ?: throw SocialHubException("recipient pubkey is required for direct message")
            social.messages().sendMessage(recipientPubkey, req.text ?: "")
        }
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
