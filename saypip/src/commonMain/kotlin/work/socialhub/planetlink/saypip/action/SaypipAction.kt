package work.socialhub.planetlink.saypip.action

import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.JsExport
import kotlinx.coroutines.launch
import work.socialhub.ksaypip.SaypipException
import work.socialhub.ksaypip.api.request.blocks.BlocksBlockRequest
import work.socialhub.ksaypip.api.request.conversations.ConversationsConversationRequest
import work.socialhub.ksaypip.api.request.conversations.ConversationsListRequest
import work.socialhub.ksaypip.api.request.conversations.ConversationsReplyRequest
import work.socialhub.ksaypip.api.request.feed.FeedFeedRequest
import work.socialhub.ksaypip.api.request.feed.FeedSearchRequest
import work.socialhub.ksaypip.api.request.media.MediaUploadRequest
import work.socialhub.ksaypip.api.request.me.MeMeRequest
import work.socialhub.ksaypip.api.request.me.MePostsRequest
import work.socialhub.ksaypip.api.request.me.MeUpdateProfileRequest
import work.socialhub.ksaypip.api.request.mutes.MutesMuteRequest
import work.socialhub.ksaypip.api.request.mutes.MutesUnmuteRequest
import work.socialhub.ksaypip.api.request.notifications.NotificationsListRequest
import work.socialhub.ksaypip.api.request.notifications.NotificationsReadRequest
import work.socialhub.ksaypip.api.request.posts.PostsCreateRequest
import work.socialhub.ksaypip.api.request.posts.PostsDeleteRequest
import work.socialhub.ksaypip.api.request.posts.PostsPostRequest
import work.socialhub.ksaypip.api.request.posts.PostsReactRequest
import work.socialhub.ksaypip.api.request.posts.PostsUnreactRequest
import work.socialhub.ksaypip.api.request.reports.ReportsReportRequest
import work.socialhub.ksaypip.api.request.users.UsersUserRequest
import work.socialhub.ksaypip.domain.MuteDuration
import work.socialhub.ksaypip.domain.RealtimeEventType
import work.socialhub.ksaypip.domain.ReportTargetType
import work.socialhub.ksaypip.entity.RealtimeEvent
import work.socialhub.ksaypip.stream.SaypipEx.stream
import work.socialhub.ksaypip.stream.listener.LifeCycleListener
import work.socialhub.ksaypip.stream.listener.RoomStreamListener
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.DeleteCommentCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.StreamActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Stream
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.ProfileForm
import work.socialhub.planetlink.saypip.define.SaypipReactionType
import work.socialhub.planetlink.saypip.model.SaypipComment
import work.socialhub.planetlink.saypip.model.SaypipPaging
import work.socialhub.planetlink.saypip.model.SaypipStream
import work.socialhub.planetlink.saypip.model.SaypipUser
import work.socialhub.planetlink.utils.ExceptionHandler
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.model.event.IdentifyEvent

/**
 * Saypip adapter.
 *
 * What Saypip has, this action maps as it is: the room as the home timeline, a post as a
 * comment, a conversation as a thread, an emoji as a reaction, a friend request's outcome as a
 * relationship. What it deliberately does not have — follows, a social graph, search of people,
 * re-sharing, bookmarks, polls, editing, a realtime socket an application may hold — is not
 * advertised and answers [NotSupportedException], because the honest answer to "can you" is the
 * one a caller can plan around.
 *
 * A person is addressed by the viewer-scoped identity token the viewer holds for them, and the
 * authenticated account itself has no token; [SaypipMapper.MY_IDENTITY] stands in for it.
 */
@JsExport
class SaypipAction(
    account: Account,
    val auth: SaypipAuth,
) : AccountActionImpl(account) {

    companion object {
        /** The picture this adapter puts where a heart would go. */
        private const val LIKE_REACTION = "❤️"

        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.MuteUser,
                SocialActionType.UnmuteUser,
                SocialActionType.BlockUser,
                SocialActionType.GetRelationship,
                SocialActionType.GetComment,
                SocialActionType.GetContext,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.LikeComment,
                SocialActionType.UnlikeComment,
                SocialActionType.ReactionComment,
                SocialActionType.UnreactionComment,
                SocialActionType.ReportComment,
                SocialActionType.ReportUser,
                SocialActionType.UpdateProfile,
                SocialActionType.MarkNotificationsRead,
                SocialActionType.GetNotification,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.SearchTimeLine,

                MessageActionType.GetMessageThread,
                MessageActionType.GetMessageTimeLine,
                MessageActionType.PostMessage,

                StreamActionType.HomeTimeLineStream,
            )
        )
    }

    override fun capabilities(): Capabilities = CAPABILITIES

    // ============================================================== //
    // Account
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun userMe(): User {
        return fetchUserMe()
    }

    /**
     * Overrides the base `userMeWithCache()` and routes both it and `userMe()` through this
     * private function to avoid the Kotlin/JS yield* crash caused by the unwired virtual suspend
     * bridge for base→abstract `userMe()` delegation. See AGENTS.md "Kotlin/JS yield* Bug".
     */
    override suspend fun userMeWithCache(): User {
        return me ?: fetchUserMe()
    }

    private suspend fun fetchUserMe(): User {
        val response = proceed {
            auth.accessor.me().me(MeMeRequest()).data
        }

        val result = SaypipMapper.me(response, service())
        me = result
        return result
    }

    /**
     * {@inheritDoc}
     * (id は相手を表す identity token)
     */
    override suspend fun user(id: Identify): User {
        return fetchUser(id)
    }

    private suspend fun fetchUser(id: Identify): User {
        val page = proceed {
            auth.accessor.users().user(
                UsersUserRequest().also {
                    it.identityToken = identityOf(id)
                },
            ).data
        }

        return SaypipMapper.user(page, service())
    }

    /**
     * {@inheritDoc}
     * https://saypip.app/users/vi_tok_...
     */
    override suspend fun user(url: String): User {
        return fetchUser(Identify(service(), ID(tokenFromUrl(url))))
    }

    /**
     * {@inheritDoc}
     *
     * A friendship in Saypip is requested and accepted between the two people; there is no
     * one-directional follow, so this is not supported.
     */
    override suspend fun followUser(id: Identify) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unfollowUser(id: Identify) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun muteUser(id: Identify) {
        proceedUnit {
            auth.accessor.mutes().mute(
                MutesMuteRequest().also {
                    it.identity = identityOf(id)
                    it.duration = MuteDuration.FOREVER
                },
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unmuteUser(id: Identify) {
        proceedUnit {
            auth.accessor.mutes().unmute(
                MutesUnmuteRequest().also {
                    it.identityToken = identityOf(id)
                },
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun blockUser(id: Identify) {
        proceedUnit {
            auth.accessor.blocks().block(
                BlocksBlockRequest().also {
                    it.identity = identityOf(id)
                },
            )
        }
    }

    /**
     * {@inheritDoc}
     *
     * Block is forget plus avoid and there is no door back: ending it is not a thing this API
     * offers.
     */
    override suspend fun unblockUser(id: Identify) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun relationship(id: Identify): Relationship {
        if (id is SaypipUser && id.relationship != null) {
            return id.relationship!!
        }

        val user = fetchUser(id)
        if (user is SaypipUser && user.relationship != null) {
            return user.relationship!!
        }
        throw IllegalStateException()
    }

    /**
     * {@inheritDoc}
     *
     * An avatar or banner travels the same way a post picture does: it is uploaded first, and
     * the profile write names the media.
     */
    override suspend fun updateProfile(form: ProfileForm) {
        val request = MeUpdateProfileRequest()

        form.displayName?.let { request.displayName = it }
        form.description?.let { request.bio = it }
        form.avatar?.let { request.avatarMediaId = uploadMedia(it, form.avatarName) }
        form.banner?.let { request.bannerMediaId = uploadMedia(it, form.bannerName) }

        proceedUnit {
            auth.accessor.me().updateProfile(request)
        }
    }

    // ============================================================== //
    // User
    // ============================================================== //
    /**
     * {@inheritDoc}
     *
     * Saypip has no social graph and no user search, by design: each of those is a
     * re-identification tool in a product like this one.
     */
    override suspend fun followingUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException()
    }

    override suspend fun followerUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException()
    }

    override suspend fun searchUsers(query: String, paging: Paging): Pageable<User> {
        throw NotSupportedException()
    }

    // ============================================================== //
    // TimeLine
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun homeTimeLine(paging: Paging): Pageable<Comment> {
        val feed = proceed {
            auth.accessor.feed().feed(
                FeedFeedRequest().also {
                    it.cursor = cursor(paging)
                    it.limit = paging.count
                },
            ).data
        }

        return SaypipMapper.timeLine(feed, service(), paging)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun mentionTimeLine(paging: Paging): Pageable<Comment> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userCommentTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        if (isMe(id)) {
            val feed = proceed {
                auth.accessor.me().posts(
                    MePostsRequest().also {
                        it.cursor = cursor(paging)
                        it.limit = paging.count
                    },
                ).data
            }
            return SaypipMapper.timeLine(feed, service(), paging)
        }

        val page = proceed {
            auth.accessor.users().user(
                UsersUserRequest().also {
                    it.identityToken = identityOf(id)
                    it.cursor = cursor(paging)
                    it.limit = paging.count
                },
            ).data
        }

        val pagingModel = SaypipPaging.fromPaging(paging)
        pagingModel.nextCursor = page.postsNextCursor

        return Pageable<Comment>().also { p ->
            p.entities = page.posts.map { SaypipMapper.comment(it, service()) }
            p.paging = pagingModel
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userLikeTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userMediaTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun searchTimeLine(query: String, paging: Paging): Pageable<Comment> {
        val feed = proceed {
            auth.accessor.feed().search(
                FeedSearchRequest().also {
                    it.q = query
                    it.cursor = cursor(paging)
                    it.limit = paging.count
                },
            ).data
        }

        return SaypipMapper.timeLine(feed, service(), paging)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userBookmarkTimeLine(paging: Paging): Pageable<Comment> {
        throw NotSupportedException()
    }

    // ============================================================== //
    // Notification
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun notification(
        paging: Paging,
        actions: Array<NotificationActionType>?,
    ): Pageable<Notification> {
        val list = proceed {
            auth.accessor.notifications().list(
                NotificationsListRequest().also {
                    it.cursor = cursor(paging)
                    it.limit = paging.count
                },
            ).data
        }

        val page = SaypipMapper.notifications(list, service(), paging)
        actions?.let { requested ->
            val codes = requested.map { it.code }.toSet()
            page.entities = page.entities.filter { it.action in codes }
        }
        return page
    }

    /**
     * {@inheritDoc}
     *
     * Saypip marks every reaction line read and offers no boundary short of one: a non-null
     * [upToId] has nothing to bound and is ignored.
     */
    override suspend fun markNotificationsRead(upToId: Identify?) {
        proceedUnit {
            auth.accessor.notifications().read(NotificationsReadRequest())
        }
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun postComment(req: CommentForm) {
        doPostComment(req)
    }

    // Free-standing impl so same-class callers (postMessage) don't route through the unwired JS
    // virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doPostComment(req: CommentForm) {
        val mediaIds = req.images.map { uploadMedia(it.data, it.name) }

        val request = PostsCreateRequest().also {
            it.body = req.text ?: ""
            it.mediaIds = mediaIds.toTypedArray().takeIf { ids -> ids.isNotEmpty() }
            it.wantsTalk = req.params["wantsTalk"] as? Boolean
            it.replyToPostId = req.replyId?.value<String>()
        }

        proceedUnit {
            auth.accessor.posts().create(request)
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun comment(id: Identify): Comment {
        return fetchComment(id)
    }

    private suspend fun fetchComment(id: Identify): Comment {
        if (id is SaypipComment) {
            return id
        }

        val post = proceed {
            auth.accessor.posts().post(
                PostsPostRequest().also {
                    it.postId = id.id<String>()
                },
            ).data
        }

        return SaypipMapper.comment(post, service())
    }

    /**
     * {@inheritDoc}
     * https://saypip.app/posts/p_...
     */
    override suspend fun comment(url: String): Comment {
        return fetchComment(Identify(service(), ID(tokenFromUrl(url))))
    }

    /**
     * {@inheritDoc}
     *
     * Saypip's like is the heart; the rest of the bar is [reactionComment].
     */
    override suspend fun likeComment(id: Identify) {
        doReaction(id, LIKE_REACTION)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unlikeComment(id: Identify) {
        doUnreaction(id, LIKE_REACTION)
    }

    // Free-standing impls so same-class callers (reactionComment) don't route through the
    // unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doReaction(id: Identify, reaction: String) {
        proceedUnit {
            auth.accessor.posts().react(
                PostsReactRequest().also {
                    it.postId = id.id<String>()
                    it.emoji = reaction
                },
            )
        }
    }

    private suspend fun doUnreaction(id: Identify, reaction: String) {
        proceedUnit {
            auth.accessor.posts().unreact(
                PostsUnreactRequest().also {
                    it.postId = id.id<String>()
                    it.emoji = reaction
                },
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun shareComment(id: Identify) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unshareComment(id: Identify) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reactionComment(id: Identify, reaction: String) {
        doReaction(id, resolveReaction(reaction))
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unreactionComment(id: Identify, reaction: String) {
        doUnreaction(id, resolveReaction(reaction))
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun deleteComment(id: Identify) {
        proceedUnit {
            auth.accessor.posts().delete(
                PostsDeleteRequest().also {
                    it.postId = id.id<String>()
                },
            )
        }
    }

    /**
     * {@inheritDoc}
     *
     * There is no editing: a post is what it was when it was written.
     */
    override suspend fun editComment(id: Identify, req: CommentForm) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reportComment(id: Identify, comment: String?) {
        proceedUnit {
            auth.accessor.reports().report(
                ReportsReportRequest().also {
                    it.targetType = ReportTargetType.POST
                    it.targetId = id.id<String>()
                    it.reason = comment ?: ""
                },
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reportUser(id: Identify, comment: String?) {
        doReportUser(id, comment)
    }

    private suspend fun doReportUser(id: Identify, comment: String?) {
        proceedUnit {
            auth.accessor.reports().report(
                ReportsReportRequest().also {
                    it.targetType = ReportTargetType.ACCOUNT
                    it.targetId = identityOf(id)
                    it.reason = comment ?: ""
                },
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun bookmarkComment(id: Identify) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unbookmarkComment(id: Identify) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun votePoll(id: Identify, choices: List<Int>) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     *
     * What precedes a self-reply is the post it quotes; conversations under a post are their own
     * screens rather than descendants of the comment.
     */
    override suspend fun commentContexts(id: Identify): Context {
        val post = proceed {
            auth.accessor.posts().post(
                PostsPostRequest().also {
                    it.postId = id.id<String>()
                },
            ).data
        }

        return Context().also { context ->
            context.ancestors = listOfNotNull(
                post.replyTo?.let { SaypipMapper.quotedComment(it, service()) }
            )
            context.descendants = listOf()
        }
    }

    // ============================================================== //
    // Space / Channel (List) API
    // ============================================================== //
    override suspend fun spaces(paging: Paging): Pageable<work.socialhub.planetlink.model.Space> {
        throw NotSupportedException()
    }

    override suspend fun channels(
        id: Identify,
        paging: Paging,
    ): Pageable<work.socialhub.planetlink.model.Channel> {
        throw NotSupportedException()
    }

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException()
    }

    override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException()
    }

    override suspend fun createList(name: String, description: String?): work.socialhub.planetlink.model.Channel {
        throw NotSupportedException()
    }

    override suspend fun addUserToList(channel: Identify, user: Identify) {
        throw NotSupportedException()
    }

    override suspend fun removeUserFromList(channel: Identify, user: Identify) {
        throw NotSupportedException()
    }

    // ============================================================== //
    // Message API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun messageThread(paging: Paging): Pageable<Thread> {
        val list = proceed {
            auth.accessor.conversations().list(
                ConversationsListRequest().also {
                    it.cursor = cursor(paging)
                    it.limit = paging.count
                },
            ).data
        }

        val page = SaypipMapper.threads(list.items.toList(), service(), paging)
        page.paging = SaypipPaging.fromPaging(paging).also { it.nextCursor = list.nextCursor }
        return page
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun messageTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        val conversation = proceed {
            auth.accessor.conversations().conversation(
                ConversationsConversationRequest().also {
                    it.conversationId = id.id<String>()
                    it.cursor = cursor(paging)
                    it.limit = paging.count
                },
            ).data
        }

        return SaypipMapper.comments(conversation, service(), paging)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun postMessage(req: CommentForm) {
        doPostMessage(req)
    }

    private suspend fun doPostMessage(req: CommentForm) {
        val conversationId = req.replyId?.value<String>()
            ?: throw NotSupportedException(
                "A conversation id is required to post a message (set replyId)."
            )

        proceedUnit {
            auth.accessor.conversations().reply(
                ConversationsReplyRequest().also {
                    it.conversationId = conversationId
                    it.body = req.text
                },
            )
        }
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * {@inheritDoc}
     *
     * The Global Room, as the socket the browser sits in: a frame says a post exists, and the
     * post itself is read back through the API before the listener sees it. There is no resume
     * and no replay, so a reconnect is answered by the feed's own refetch.
     */
    override suspend fun setHomeTimeLineStream(callback: EventCallback): Stream {
        return openRoomStream(callback)
    }

    // Free-standing impl so the override doesn't route through the unwired JS virtual suspend
    // bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun openRoomStream(callback: EventCallback): Stream {
        val room = auth.accessor.stream().roomStream()
        val stream = SaypipStream(room)

        room.register(
            SaypipRoomListener(callback, this, stream.scope),
            SaypipConnectionListener(callback),
        )

        return stream
    }

    /**
     * {@inheritDoc}
     *
     * A reaction or a reply reaches the room as nothing: the socket carries post IDs, and a
     * notification is about something that happened to a post rather than a posted one.
     */
    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     *
     * The room reports a post arriving or leaving, not the reactions and conversations around
     * it, so a comment already held has nothing here to update it.
     */
    override suspend fun setCommentUpdateStream(
        comments: List<Comment>,
        callback: EventCallback,
    ): work.socialhub.planetlink.model.CommentUpdateStream {
        throw NotSupportedException()
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    private fun service(): Service {
        return account.service
    }

    private fun isMe(id: Identify): Boolean {
        if (id is SaypipUser) {
            return id.identityToken.isEmpty()
        }
        return id.id?.value<String>() == SaypipMapper.MY_IDENTITY
    }

    private fun identityOf(id: Identify): String {
        if (id is SaypipUser && id.identityToken.isNotEmpty()) {
            return id.identityToken
        }
        if (id is SaypipUser && id.identityToken.isEmpty()) {
            throw NotSupportedException(
                "The authenticated account has no identity token of its own."
            )
        }
        return id.id<String>()
    }

    private fun cursor(paging: Paging?): String? {
        return (paging as? SaypipPaging)?.cursor
    }

    private fun tokenFromUrl(url: String): String {
        return url.substringBefore('?')
            .substringBefore('#')
            .trimEnd('/')
            .substringAfterLast('/')
    }

    private fun resolveReaction(reaction: String): String {
        val type = reaction.lowercase()
        if (SaypipReactionType.Like.codes.contains(type)) {
            return LIKE_REACTION
        }
        return reaction
    }

    private suspend fun uploadMedia(data: ByteArray, name: String?): String {
        val media = proceed {
            auth.accessor.media().upload(
                MediaUploadRequest().also {
                    it.data = data
                    it.contentType = mediaContentType(name)
                },
            ).data
        }
        return media.id
    }

    private fun mediaContentType(name: String?): String {
        return when (name?.substringAfterLast('.', "")?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }
    }

    // ============================================================== //
    // Error handling
    // ============================================================== //
    private suspend fun <T> proceed(runner: suspend () -> T): T {
        try {
            return runner()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if ((e as? SaypipException)?.status == 401 && auth.canRefresh()) {
                // The refusal may be the request's or the refresh's; both are classified the
                // same way rather than leaking a raw client exception.
                try {
                    auth.refreshAccessToken()
                } catch (e2: CancellationException) {
                    throw e2
                } catch (e2: Exception) {
                    throw classify(e2)
                }
                return try {
                    runner()
                } catch (e2: CancellationException) {
                    throw e2
                } catch (e2: Exception) {
                    throw classify(e2)
                }
            }
            throw classify(e)
        }
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        proceed {
            runner()
        }
    }

    private fun classify(e: Exception): SocialHubException {
        return ExceptionHandler.classify(
            e = e,
            serviceType = ServiceType.Saypip,
            statusCode = (e as? SaypipException)?.status,
            responseBody = (e as? SaypipException)?.body,
        )
    }

    // ============================================================== //
    // Stream listeners
    // ============================================================== //
    // A frame is a post ID: the post is read back before the callback, and a read that the
    // visibility rules do not answer with (taken down since the frame, or from an account no
    // read path serves) is dropped rather than drawn.
    internal class SaypipRoomListener(
        private val listener: EventCallback,
        private val action: SaypipAction,
        private val scope: kotlinx.coroutines.CoroutineScope,
    ) : RoomStreamListener {

        override fun onEvent(event: RealtimeEvent) {
            when (event.type) {
                RealtimeEventType.POST_CREATED -> onPostCreated(event.postId)
                RealtimeEventType.POST_DELETED -> onPostDeleted(event.postId)
            }
        }

        private fun onPostCreated(postId: String) {
            if (listener !is UpdateCommentCallback) return

            scope.launch {
                try {
                    val comment = action.comment(
                        Identify(action.account.service, ID(postId)),
                    )
                    listener.onUpdate(CommentEvent(comment))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // The read did not answer with the post; it leaves rather than being drawn.
                }
            }
        }

        private fun onPostDeleted(postId: String) {
            if (listener is DeleteCommentCallback) {
                listener.onDelete(IdentifyEvent(postId))
            }
        }
    }

    internal class SaypipConnectionListener(
        private val listener: EventCallback,
    ) : LifeCycleListener {

        override fun onConnect() {
            if (listener is ConnectCallback) {
                listener.onConnect()
            }
        }

        override fun onDisconnect() {
            if (listener is DisconnectCallback) {
                listener.onDisconnect()
            }
        }

        override fun onError(e: Exception) {
            if (listener is ErrorCallback) {
                val classified = if (e is SocialHubException) e
                else ExceptionHandler.classify(
                    e = e,
                    serviceType = ServiceType.Saypip,
                    statusCode = (e as? SaypipException)?.status,
                    responseBody = (e as? SaypipException)?.body,
                )
                listener.onError(classified)
            }
        }
    }
}
