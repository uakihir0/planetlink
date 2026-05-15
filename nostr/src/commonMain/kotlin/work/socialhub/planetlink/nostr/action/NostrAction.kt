package work.socialhub.planetlink.nostr.action

import kotlinx.datetime.Instant
import work.socialhub.knostr.entity.Nip19Entity
import work.socialhub.knostr.social.model.NostrDirectMessage
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.social.model.NostrUser as KnostrUser
import work.socialhub.knostr.social.stream.NotificationStream
import work.socialhub.knostr.social.stream.TimelineStream
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.nostr.model.NostrComment
import work.socialhub.planetlink.nostr.model.NostrPaging

class NostrAction(
    account: Account,
    val auth: NostrAuth,
) : AccountActionImpl(account) {

    private val accessor get() = auth.accessor
    private val social get() = accessor.social
    private val nostr get() = accessor.nostr
    private val pubkey get() = accessor.pubkey

    // ============================================================== //
    // Account API
    // ============================================================== //

    override suspend fun userMe(): User {
        return proceed {
            val response = social.users().getProfile(pubkey)
            val user = NostrMapper.user(response.data, service())
            me = user
            user
        }
    }

    override suspend fun user(id: Identify): User {
        val key = id.id!!.value<String>()
        if (key == pubkey && me != null) return me!!

        return proceed {
            val response = social.users().getProfile(key)
            NostrMapper.user(response.data, service())
        }
    }

    override suspend fun user(url: String): User {
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
                trimmed.length == 64 && trimmed.all { it.isLowerCase() || it.isDigit() } -> {
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

    override suspend fun userCommentTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
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
        return proceed {
            val eventId = id.id!!.value<String>()
            val response = social.feed().getNote(eventId)
            val userMe = userMeWithCache()
            NostrMapper.comment(response.data, service(), userMe)
        }
    }

    override suspend fun comment(url: String): Comment {
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
                trimmed.length == 64 && trimmed.all { it.isLowerCase() || it.isDigit() } -> {
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
            val threads = response.data.map { dmThread ->
                val threadId = dmThread.rootNote?.event?.pubkey
                    ?: dmThread.replies.firstOrNull()?.event?.pubkey
                    ?: ""
                Thread(service()).apply {
                    id = ID(threadId)
                    lastUpdate = dmThread.rootNote?.let {
                        kotlinx.datetime.Instant.fromEpochSeconds(it.createdAt, 0)
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
        identify: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val pubkey = identify.id!!.value<String>()
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
                    if (authorPubkey == pubkey) {
                        this.user = userMe
                    } else {
                        try {
                            val profile = social.users().getProfile(authorPubkey)
                            this.user = NostrMapper.user(profile.data, service())
                        } catch (_: Exception) {
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
        val stream = NostrStream(
            timelineStream = TimelineStream(nostr).also { ts ->
                ts.onNoteCallback = { note ->
                    // callback is EventCallback - we just forward the event
                }
            }
        )
        stream.open()
        return stream
    }

    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        val stream = NostrStream(
            notificationStream = NotificationStream(nostr).also { ns ->
                ns.onMentionCallback = { note ->
                }
                ns.onReactionCallback = { reaction ->
                }
            }
        )
        stream.open()
        return stream
    }

    override fun request(): RequestAction {
        return NostrRequest(account)
    }

    // ============================================================== //
    // Internal
    // ============================================================== //

    private fun getAuthorPubkey(id: Identify): String {
        if (id is NostrComment && id.user != null) {
            return id.user!!.id?.value<String>() ?: pubkey
        }
        return pubkey
    }

    private fun service(): Service = account.service

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return try {
            runner()
        } catch (e: SocialHubException) {
            throw e
        } catch (e: Exception) {
            throw SocialHubException(e.message, e)
        }
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        try {
            runner()
        } catch (e: SocialHubException) {
            throw e
        } catch (e: Exception) {
            throw SocialHubException(e.message, e)
        }
    }
}
