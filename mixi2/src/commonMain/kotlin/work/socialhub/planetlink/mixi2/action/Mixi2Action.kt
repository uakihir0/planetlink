package work.socialhub.planetlink.mixi2.action

import kotlin.js.JsExport
import work.socialhub.kmixi2web.Mixi2Web
import work.socialhub.kmixi2web.Mixi2WebException
import work.socialhub.kmixi2web.api.request.AddStampToPostRequest
import work.socialhub.kmixi2web.api.request.ApproveFollowingRequestRequest
import work.socialhub.kmixi2web.api.request.CreateBookmarkRequest
import work.socialhub.kmixi2web.api.request.CreateFollowingRequest
import work.socialhub.kmixi2web.api.request.CreateLikeRequest
import work.socialhub.kmixi2web.api.request.CreatePostRequest
import work.socialhub.kmixi2web.api.request.DeleteBookmarkRequest
import work.socialhub.kmixi2web.api.request.DeleteFollowingRequest
import work.socialhub.kmixi2web.api.request.DeleteLikeRequest
import work.socialhub.kmixi2web.api.request.DeletePostRequest
import work.socialhub.kmixi2web.api.request.DeleteRepostRequest
import work.socialhub.kmixi2web.api.request.GetChatRoomMessagesRequest
import work.socialhub.kmixi2web.api.request.GetChatRoomsRequest
import work.socialhub.kmixi2web.api.request.GetCommunityTimelineRequest
import work.socialhub.kmixi2web.api.request.GetFollowersRequest
import work.socialhub.kmixi2web.api.request.GetFollowingsRequest
import work.socialhub.kmixi2web.api.request.GetFollowingsTimelineRequest
import work.socialhub.kmixi2web.api.request.GetHashtagTimelineRequest
import work.socialhub.kmixi2web.api.request.GetNotificationsRequest
import work.socialhub.kmixi2web.api.request.GetParticipatingCommunitiesRequest
import work.socialhub.kmixi2web.api.request.GetParticipatingCommunityMembersRequest
import work.socialhub.kmixi2web.api.request.GetPendingFollowingRequestsRequest
import work.socialhub.kmixi2web.api.request.GetPersonaByNameRequest
import work.socialhub.kmixi2web.api.request.GetPersonalTimelineRequest
import work.socialhub.kmixi2web.api.request.GetPersonasRequest
import work.socialhub.kmixi2web.api.request.GetPostRequest
import work.socialhub.kmixi2web.api.request.GetProfileByNameRequest
import work.socialhub.kmixi2web.api.request.GetProfileRequest
import work.socialhub.kmixi2web.api.request.GetReactionPostsRequest
import work.socialhub.kmixi2web.api.request.GetRecommendedTimelineRequest
import work.socialhub.kmixi2web.api.request.GetRepliesRequest
import work.socialhub.kmixi2web.api.request.GetReplyAncestorsRequest
import work.socialhub.kmixi2web.api.request.GetSubscribingFeedsRequest
import work.socialhub.kmixi2web.api.request.MakePersonaBlockRequest
import work.socialhub.kmixi2web.api.request.MakePersonaMuteRequest
import work.socialhub.kmixi2web.api.request.MakePersonaUnblockRequest
import work.socialhub.kmixi2web.api.request.MakePersonaUnmuteRequest
import work.socialhub.kmixi2web.api.request.MarkNotificationAsReadRequest
import work.socialhub.kmixi2web.api.request.MarkNotificationsAsReadBeforeTimeRequest
import work.socialhub.kmixi2web.api.request.RemoveStampFromPostRequest
import work.socialhub.kmixi2web.api.request.ReportPersonaRequest
import work.socialhub.kmixi2web.api.request.ReportPostRequest
import work.socialhub.kmixi2web.api.request.RejectFollowingRequestRequest
import work.socialhub.kmixi2web.api.request.SearchRequest
import work.socialhub.kmixi2web.api.request.SendFollowingRequestRequest
import work.socialhub.kmixi2web.api.request.SendMessageToRoomRequest
import work.socialhub.kmixi2web.api.request.UpdateProfileRequest
import work.socialhub.kmixi2web.api.request.UploadMediaRequest
import work.socialhub.kmixi2web.entity.MediaCategory
import work.socialhub.kmixi2web.entity.NotificationActivityType
import work.socialhub.kmixi2web.entity.Persona
import work.socialhub.kmixi2web.entity.PersonaConnectivity
import work.socialhub.kmixi2web.entity.Post
import work.socialhub.kmixi2web.entity.PostReactionType
import work.socialhub.kmixi2web.entity.Profile
import work.socialhub.kmixi2web.entity.ReportReasonType
import work.socialhub.kmixi2web.entity.SearchOperation
import work.socialhub.kmixi2web.entity.SearchType
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.CommentUpdateStream
import work.socialhub.planetlink.model.Context
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Space
import work.socialhub.planetlink.model.Stream
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.ClientException
import work.socialhub.planetlink.model.error.NotFoundException
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.MediaForm
import work.socialhub.planetlink.model.request.ProfileForm
import work.socialhub.planetlink.mixi2.define.Mixi2ActionType
import work.socialhub.planetlink.mixi2.define.Mixi2ReactionType
import work.socialhub.planetlink.mixi2.model.Mixi2Comment
import work.socialhub.planetlink.mixi2.model.Mixi2Paging
import work.socialhub.planetlink.mixi2.model.Mixi2User
import work.socialhub.planetlink.utils.ExceptionHandler

/**
 * mixi2 adapter.
 *
 * mixi2 is reached through its undocumented web protobuf RPC interface
 * (kmixi2web): a post is a comment, a persona is a user, a like is a like and a
 * custom stamp is a reaction, a community is a channel, a chat room is a
 * thread, and the subscribing feed is the home timeline. What mixi2 does not
 * have — editing, polls, a mention timeline, a stream socket — is not
 * advertised and answers [NotSupportedException].
 *
 * Every write is addressed by persona id; a persona name (the `@handle`) is
 * resolved first where a caller passes one. Timeline authors are read back in a
 * single batch `GetPersonas` call and cached for the account.
 */
@JsExport
class Mixi2Action(
    account: Account,
    val auth: Mixi2Auth,
    private val client: Mixi2Web,
) : AccountActionImpl(account) {

    private val personaCache = mutableMapOf<String, Mixi2User>()
    private var activePersonaId: String? = null

    companion object {
        private const val DEFAULT_COUNT = 20
        private const val DEFAULT_CONTEXT_COUNT = 100
        private val UUID_REGEX = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        )

        /**
         * The types a default notification read returns: everything but the
         * mentions, replies and quotes the contract leaves out.
         */
        private val DEFAULT_ACTIVITY_TYPES = NotificationActivityType.entries.filter {
            it != NotificationActivityType.REPLY &&
                it != NotificationActivityType.MENTION &&
                it != NotificationActivityType.QUOTE &&
                it != NotificationActivityType.UNKNOWN
        }

        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.FollowUser,
                SocialActionType.UnfollowUser,
                SocialActionType.MuteUser,
                SocialActionType.UnmuteUser,
                SocialActionType.BlockUser,
                SocialActionType.UnblockUser,
                SocialActionType.GetRelationship,
                SocialActionType.AcceptFollowRequest,
                SocialActionType.RejectFollowRequest,
                SocialActionType.ReportUser,
                SocialActionType.UpdateProfile,
                SocialActionType.GetComment,
                SocialActionType.GetContext,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.LikeComment,
                SocialActionType.UnlikeComment,
                SocialActionType.ShareComment,
                SocialActionType.UnShareComment,
                SocialActionType.ReactionComment,
                SocialActionType.UnreactionComment,
                SocialActionType.ReportComment,
                SocialActionType.BookmarkComment,
                SocialActionType.UnbookmarkComment,
                SocialActionType.GetUserBookmarks,
                SocialActionType.GetNotification,
                SocialActionType.MarkNotificationsRead,
                SocialActionType.GetChannels,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.UserLikeTimeLine,
                TimeLineActionType.UserMediaTimeLine,
                TimeLineActionType.SearchTimeLine,
                TimeLineActionType.UserBookmarkTimeLine,
                TimeLineActionType.ChannelTimeLine,
                TimeLineActionType.MessageTimeLine,

                UsersActionType.GetFollowingUsers,
                UsersActionType.GetFollowerUsers,
                UsersActionType.SearchUsers,
                UsersActionType.ChannelUsers,

                MessageActionType.GetMessageThread,
                MessageActionType.GetMessageTimeLine,
                MessageActionType.PostMessage,

                Mixi2ActionType.RecommendedTimeLine,
                Mixi2ActionType.FollowingTimeLine,
                Mixi2ActionType.HashtagTimeLine,
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
     * Overrides the base `userMeWithCache()`; both it and `userMe()` route
     * through a private fetcher to avoid the Kotlin/JS yield* bridge crash.
     */
    override suspend fun userMeWithCache(): User {
        return me ?: fetchUserMe()
    }

    private suspend fun fetchUserMe(): Mixi2User {
        val session = proceed { client.session().getSession().data }
        val activeId = session.activePersonaId?.takeIf { it.isNotBlank() }
        val managed = if (activeId != null) {
            session.sessionManagedPersonas.firstOrNull {
                it.profile?.persona?.personaId == activeId
            }
        } else {
            session.sessionManagedPersonas.firstOrNull { it.profile?.persona != null }
        }
        val profile = managed?.profile
            ?: throw NotFoundException(null, "The mixi2 session carries no persona.", null)

        val user = Mixi2Mapper.user(profile, service())
        activePersonaId = user.personaId
        personaCache[user.personaId] = user
        me = user
        return user
    }

    /**
     * {@inheritDoc}
     * (id はペルソナ ID、または `@` を除いたペルソナ名)
     */
    override suspend fun user(id: Identify): User {
        return fetchUser(id)
    }

    /**
     * An explicit user read always fetches the profile, so a refresh sees the
     * current counts and relationship; the result also warms the batch cache
     * used for timeline authors.
     */
    private suspend fun fetchUser(id: Identify): Mixi2User {
        return Mixi2Mapper.user(fetchProfile(id), service()).also {
            personaCache[it.personaId] = it
        }
    }

    private suspend fun fetchProfile(id: Identify): Profile {
        val personaId = resolvePersonaId(id)
        return proceed {
            client.persona().getProfile(GetProfileRequest(personaId)).data.profile
        } ?: throw NotFoundException(null, "The mixi2 persona was not found.", null)
    }

    private suspend fun fetchProfileByName(name: String): Profile {
        return proceed {
            client.persona().getProfileByName(GetProfileByNameRequest(name)).data.profile
        } ?: throw NotFoundException(null, "The mixi2 persona was not found.", null)
    }

    /**
     * {@inheritDoc}
     * https://mixi.social/@handle
     */
    override suspend fun user(url: String): User {
        val name = segments(url).firstOrNull { it.startsWith("@") }
            ?.removePrefix("@")
            ?.takeIf { it.isNotEmpty() }
            ?: throw NotSupportedException("The URL is not a mixi2 persona URL.")
        return Mixi2Mapper.user(fetchProfileByName(name), service()).also {
            personaCache[it.personaId] = it
        }
    }

    /**
     * {@inheritDoc}
     *
     * A persona that approves followers manually refuses [createFollowing];
     * the follow request is the write that works for those personas.
     */
    override suspend fun followUser(id: Identify) {
        doFollowUser(id)
    }

    private suspend fun doFollowUser(id: Identify) {
        val personaId = resolvePersonaId(id)
        try {
            proceedUnit {
                client.follow().createFollowing(CreateFollowingRequest(personaId))
            }
        } catch (_: ClientException) {
            proceedUnit {
                client.follow().sendFollowingRequest(SendFollowingRequestRequest(personaId))
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unfollowUser(id: Identify) {
        val personaId = resolvePersonaId(id)
        proceedUnit {
            client.follow().deleteFollowing(DeleteFollowingRequest(personaId))
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun muteUser(id: Identify) {
        val personaId = resolvePersonaId(id)
        proceedUnit {
            client.moderation().mutePersona(MakePersonaMuteRequest(personaId))
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unmuteUser(id: Identify) {
        val personaId = resolvePersonaId(id)
        proceedUnit {
            client.moderation().unmutePersona(MakePersonaUnmuteRequest(personaId))
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun blockUser(id: Identify) {
        val personaId = resolvePersonaId(id)
        proceedUnit {
            client.moderation().blockPersona(MakePersonaBlockRequest(personaId))
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unblockUser(id: Identify) {
        val personaId = resolvePersonaId(id)
        proceedUnit {
            client.moderation().unblockPersona(MakePersonaUnblockRequest(personaId))
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun relationship(id: Identify): Relationship {
        return Mixi2Mapper.relationship(fetchProfile(id))
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun acceptFollowRequest(id: Identify) {
        decideFollowRequest(id, approve = true)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun rejectFollowRequest(id: Identify) {
        decideFollowRequest(id, approve = false)
    }

    private suspend fun decideFollowRequest(id: Identify, approve: Boolean) {
        val personaId = resolvePersonaId(id)
        val requestId = findPendingFollowRequest(personaId)
            ?: throw NotFoundException(
                null,
                "No pending mixi2 follow request from that persona.",
                null,
            )

        proceedUnit {
            if (approve) {
                client.follow().approveFollowingRequest(ApproveFollowingRequestRequest(requestId))
            } else {
                client.follow().rejectFollowingRequest(RejectFollowingRequestRequest(requestId))
            }
        }
    }

    /**
     * The pending requests are paged, so a request past the first page is still
     * found by walking the cursor to its end.
     */
    private suspend fun findPendingFollowRequest(personaId: String): String? {
        var cursor: String? = null
        while (true) {
            val response = proceed {
                client.follow().getPendingFollowingRequests(
                    GetPendingFollowingRequestsRequest().also { it.cursor = cursor }
                ).data
            }
            response.followingRequests.firstOrNull { it.senderId == personaId }
                ?.let { return it.requestId }

            val next = response.nextCursor?.takeIf { it.isNotBlank() } ?: return null
            if (next == cursor) return null
            cursor = next
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reportUser(id: Identify, comment: String?) {
        val personaId = resolvePersonaId(id)
        proceedUnit {
            client.moderation().reportPersona(
                ReportPersonaRequest(
                    personaId = personaId,
                    reasonType = ReportReasonType.OTHER,
                    reasonContent = comment.orEmpty(),
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     *
     * mixi2's profile update carries a display name, a profile text, a status
     * and a link; it has no avatar or banner write.
     */
    override suspend fun updateProfile(form: ProfileForm) {
        if (form.avatar != null || form.banner != null) {
            throw NotSupportedException(
                "The mixi2 profile update carries no avatar or banner."
            )
        }

        val request = UpdateProfileRequest()
        form.displayName?.let { request.displayName = it }
        form.description?.let { request.profileText = it }
        if (request.displayName == null && request.profileText == null) return

        proceedUnit {
            client.persona().updateProfile(request)
        }
    }

    // ============================================================== //
    // User
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun followingUsers(id: Identify, paging: Paging): Pageable<User> {
        return fetchFollowGraph(id, paging, followers = false)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun followerUsers(id: Identify, paging: Paging): Pageable<User> {
        return fetchFollowGraph(id, paging, followers = true)
    }

    private suspend fun fetchFollowGraph(
        id: Identify,
        paging: Paging,
        followers: Boolean,
    ): Pageable<User> {
        val personaId = resolvePersonaId(id)
        return if (followers) {
            val response = proceed {
                client.follow().getFollowers(
                    GetFollowersRequest().also {
                        it.personaId = personaId
                        it.cursorId = cursor(paging)
                        it.limit = limit(paging)
                    }
                ).data
            }
            followPage(
                entries = response.followers.map { it.persona to it.connectivity },
                nextCursor = response.cursorId.ifBlank { null },
                paging = paging,
            )
        } else {
            val response = proceed {
                client.follow().getFollowings(
                    GetFollowingsRequest().also {
                        it.personaId = personaId
                        it.cursorId = cursor(paging)
                        it.limit = limit(paging)
                    }
                ).data
            }
            followPage(
                entries = response.followings.map { it.persona to it.connectivity },
                nextCursor = response.cursorId.ifBlank { null },
                paging = paging,
            )
        }
    }

    private fun followPage(
        entries: List<Pair<Persona?, PersonaConnectivity?>>,
        nextCursor: String?,
        paging: Paging,
    ): Pageable<User> {
        return Pageable<User>().also { p ->
            p.entities = entries.mapNotNull { (persona, connectivity) ->
                persona?.let { u -> Mixi2Mapper.user(u, service(), connectivity) }
            }
            p.paging = Mixi2Paging.fromPaging(paging).also { it.nextCursor = nextCursor }
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun searchUsers(query: String, paging: Paging): Pageable<User> {
        val response = proceed {
            client.search().search(
                SearchRequest(
                    query = query,
                    operations = listOf(
                        SearchOperation(
                            type = SearchType.PERSONAS,
                            operationId = 1,
                            untilCursor = cursor(paging),
                            limit = limit(paging),
                        )
                    ),
                )
            ).data
        }

        val result = response.results.firstOrNull { it.operationId == 1 }?.personasResult
        return Mixi2Mapper.users(
            sources = result?.personaWithConnectivities.orEmpty(),
            service = service(),
            paging = paging,
            nextCursor = result?.nextCursor?.ifBlank { null },
        )
    }

    // ============================================================== //
    // TimeLine
    // ============================================================== //
    /**
     * {@inheritDoc}
     *
     * The home timeline is the subscribing feed, which mixes the followed
     * personas with the communities the account belongs to.
     */
    override suspend fun homeTimeLine(paging: Paging): Pageable<Comment> {
        val response = proceed {
            client.timeline().getSubscribingFeeds(
                GetSubscribingFeedsRequest(
                    untilCursor = cursor(paging),
                    limit = limit(paging),
                )
            ).data
        }
        val posts = response.feeds.mapNotNull {
            it.post ?: it.communityAggregationPost?.post
        }
        return timeline(posts, paging, response.nextCursor?.ifBlank { null })
    }

    /**
     * おすすめタイムラインを取得
     */
    suspend fun recommendedTimeLine(paging: Paging): Pageable<Comment> {
        return fetchPostsTimeline(paging) {
            client.timeline().getRecommendedTimeline(
                GetRecommendedTimelineRequest(
                    untilCursorId = cursor(paging),
                    limit = limit(paging),
                )
            ).data.posts
        }
    }

    /**
     * フォロー中タイムラインを取得
     */
    suspend fun followingTimeLine(paging: Paging): Pageable<Comment> {
        return fetchPostsTimeline(paging) {
            client.timeline().getFollowingsTimeline(
                GetFollowingsTimelineRequest(
                    untilCursorId = cursor(paging),
                    limit = limit(paging),
                )
            ).data.posts
        }
    }

    /**
     * ハッシュタグタイムラインを取得
     */
    suspend fun hashtagTimeLine(hashtag: String, paging: Paging): Pageable<Comment> {
        return fetchPostsTimeline(paging) {
            client.timeline().getHashtagTimeline(
                GetHashtagTimelineRequest(
                    hashtag = hashtag.removePrefix("#"),
                    untilCursorId = cursor(paging),
                    limit = limit(paging),
                )
            ).data.posts
        }
    }

    /**
     * {@inheritDoc}
     *
     * mixi2 has no mention timeline; notifications are its mention surface.
     */
    override suspend fun mentionTimeLine(paging: Paging): Pageable<Comment> {
        throw NotSupportedException("mixi2 has no mention timeline.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userCommentTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return fetchPersonalTimeLine(id, paging, mediaOnly = false)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userMediaTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return fetchPersonalTimeLine(id, paging, mediaOnly = true)
    }

    private suspend fun fetchPersonalTimeLine(
        id: Identify,
        paging: Paging,
        mediaOnly: Boolean,
    ): Pageable<Comment> {
        val personaId = resolvePersonaId(id)
        return fetchPostsTimeline(paging) {
            client.timeline().getPersonalTimeline(
                GetPersonalTimelineRequest(
                    personaId = personaId,
                    untilCursorId = cursor(paging),
                    limit = limit(paging),
                    mediaOnly = mediaOnly,
                )
            ).data.posts
        }
    }

    /**
     * {@inheritDoc}
     *
     * The reaction-post lookup reads the active persona's own liked posts, so
     * another persona's likes are not readable.
     */
    override suspend fun userLikeTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        requireActivePersona(
            id,
            "mixi2 reads back the active persona's liked posts only."
        )
        return fetchReactionPosts(PostReactionType.LIKE, paging)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userBookmarkTimeLine(paging: Paging): Pageable<Comment> {
        return fetchReactionPosts(PostReactionType.BOOKMARK, paging)
    }

    private suspend fun fetchReactionPosts(
        type: PostReactionType,
        paging: Paging,
    ): Pageable<Comment> {
        val response = proceed {
            client.timeline().getReactionPosts(
                GetReactionPostsRequest(
                    reactionType = type,
                    limit = limit(paging),
                    cursor = cursor(paging),
                )
            ).data
        }
        val nextCursor = if (response.hasNext) response.nextCursor.ifBlank { null } else null
        return timeline(response.posts, paging, nextCursor)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun searchTimeLine(query: String, paging: Paging): Pageable<Comment> {
        val response = proceed {
            client.search().search(
                SearchRequest(
                    query = query,
                    operations = listOf(
                        SearchOperation(
                            type = SearchType.POSTS,
                            operationId = 1,
                            untilCursor = cursor(paging),
                            limit = limit(paging),
                        )
                    ),
                )
            ).data
        }

        val result = response.results.firstOrNull { it.operationId == 1 }?.postsResult
        return timeline(
            posts = result?.posts.orEmpty(),
            paging = paging,
            nextCursor = result?.nextCursor?.ifBlank { null },
        )
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
        val types = activityTypesOf(actions)
        val response = proceed {
            client.notification().getNotifications(
                GetNotificationsRequest().also {
                    it.limit = limit(paging)
                    it.untilTimeSeriesId = cursor(paging)
                    it.activityTypes = types
                }
            ).data
        }

        val users = fetchPersonas(response.notifications.map { it.issuerId })
        val page = Mixi2Mapper.notifications(
            sources = response.notifications,
            users = users,
            service = service(),
            paging = paging,
            nextCursor = if (response.hasNext) {
                response.notifications.lastOrNull()?.timeSeriesId
            } else {
                null
            },
        )

        actions?.let { requested ->
            val codes = requested.map { it.code }.toSet()
            page.entities = page.entities.filter { it.action in codes }
        }
        return page
    }

    /**
     * {@inheritDoc}
     *
     * mixi2 marks a range before a time-series id, so the bounded case marks
     * that range and then the boundary itself, keeping "up to" inclusive.
     */
    override suspend fun markNotificationsRead(upToId: Identify?) {
        if (upToId != null) {
            proceedUnit {
                client.notification().markNotificationsAsReadBeforeTime(
                    MarkNotificationsAsReadBeforeTimeRequest(upToId.id())
                )
            }
            proceedUnit {
                client.notification().markNotificationAsRead(
                    MarkNotificationAsReadRequest(upToId.id())
                )
            }
            return
        }

        val latest = proceed {
            client.notification().getNotifications(
                GetNotificationsRequest().also { it.limit = 1 }
            ).data
        }
        val newest = latest.notifications.firstOrNull() ?: return
        proceedUnit {
            client.notification().markNotificationsAsReadBeforeTime(
                MarkNotificationsAsReadBeforeTimeRequest(newest.timeSeriesId)
            )
        }
        proceedUnit {
            client.notification().markNotificationAsRead(
                MarkNotificationAsReadRequest(newest.timeSeriesId)
            )
        }
    }

    private fun activityTypesOf(
        actions: Array<NotificationActionType>?,
    ): List<NotificationActivityType> {
        if (actions == null) return DEFAULT_ACTIVITY_TYPES
        val types = mutableListOf<NotificationActivityType>()
        for (action in actions) {
            when (action) {
                NotificationActionType.MENTION -> {
                    types.add(NotificationActivityType.REPLY)
                    types.add(NotificationActivityType.MENTION)
                }

                NotificationActionType.QUOTE -> types.add(NotificationActivityType.QUOTE)
                NotificationActionType.FOLLOW -> {
                    types.add(NotificationActivityType.FOLLOW)
                    types.add(NotificationActivityType.FOLLOWING_REQUEST_APPROVED)
                }

                NotificationActionType.FOLLOW_REQUEST -> {
                    types.add(NotificationActivityType.FOLLOWING_REQUEST_RECEIVED)
                    types.add(NotificationActivityType.INVITATION_FOLLOW)
                }

                NotificationActionType.SHARE -> types.add(NotificationActivityType.REPOST)
                NotificationActionType.LIKE -> types.add(NotificationActivityType.LIKE)
                NotificationActionType.REACTION -> types.add(NotificationActivityType.REACTION)
                else -> {}
            }
        }
        return types.distinct()
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * {@inheritDoc}
     *
     * Set `params["communityId"]` to post into a community; the same value is
     * passed to the uploads the post's images need.
     */
    override suspend fun postComment(req: CommentForm) {
        doPostComment(req)
    }

    // Free-standing impl so same-class callers don't route through the unwired
    // JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doPostComment(req: CommentForm) {
        if (req.poll != null) {
            throw NotSupportedException("mixi2 has no polls.")
        }
        if (req.isMessage) {
            doPostMessage(req)
            return
        }

        val text = req.text.orEmpty()
        if (text.isBlank() &&
            req.quoteId == null &&
            req.images.isEmpty() &&
            req.replyId == null
        ) {
            throw NotSupportedException(
                "A mixi2 post needs text, media, a quote or a reply target."
            )
        }

        val communityId = req.params["communityId"] as? String
        val mediaIds = req.images.map { uploadMedia(it, communityId) }
        val request = CreatePostRequest(text).also {
            it.inReplyToPostId = req.replyId?.value<String>()
            it.quotePostId = req.quoteId?.value<String>()
            it.mediaIds = mediaIds
            it.isSensitive = req.isSensitive
            it.communityId = communityId
        }
        proceedUnit {
            client.post().createPost(request)
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun comment(id: Identify): Comment {
        return fetchComment(id)
    }

    /**
     * A chat message is addressed by its message id, which `GetPost` does not
     * serve, so it is not refreshable; every public post is re-read.
     */
    private suspend fun fetchComment(id: Identify): Mixi2Comment {
        if (id is Mixi2Comment && id.directMessage) {
            return id
        }
        val post = proceed {
            client.post().getPost(GetPostRequest(id.id())).data.post
        } ?: throw NotFoundException(null, "The mixi2 post was not found.", null)

        return fetchComments(listOf(post)).first()
    }

    /**
     * {@inheritDoc}
     * https://mixi.social/@handle/posts/uuid
     * https://mixi.social/posts/uuid
     */
    override suspend fun comment(url: String): Comment {
        val path = segments(url)
        val index = path.indexOf("posts")
        val postId = path.getOrNull(index + 1)
            ?.takeIf { index >= 0 && it.isNotEmpty() }
            ?: throw NotSupportedException("The URL is not a mixi2 post URL.")
        return fetchComment(Identify(service(), ID(postId)))
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun likeComment(id: Identify) {
        doLikeComment(id)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unlikeComment(id: Identify) {
        doUnlikeComment(id)
    }

    // Free-standing impls so same-class callers don't route through the
    // unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doLikeComment(id: Identify) {
        proceedUnit {
            client.reaction().createLike(CreateLikeRequest(id.id()))
        }
    }

    private suspend fun doUnlikeComment(id: Identify) {
        proceedUnit {
            client.reaction().deleteLike(DeleteLikeRequest(id.id()))
        }
    }

    /**
     * {@inheritDoc}
     *
     * The like aliases in [Mixi2ReactionType] write a like; every other
     * reaction is the id of a mixi2 stamp.
     */
    override suspend fun reactionComment(id: Identify, reaction: String) {
        if (Mixi2ReactionType.Like.codes.contains(reaction.lowercase())) {
            doLikeComment(id)
            return
        }
        doAddStamp(id, reaction)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unreactionComment(id: Identify, reaction: String) {
        if (Mixi2ReactionType.Like.codes.contains(reaction.lowercase())) {
            doUnlikeComment(id)
            return
        }
        doRemoveStamp(id, reaction)
    }

    private suspend fun doAddStamp(id: Identify, stampId: String) {
        proceedUnit {
            client.reaction().addStampToPost(
                AddStampToPostRequest(postId = id.id(), stampId = stampId)
            )
        }
    }

    private suspend fun doRemoveStamp(id: Identify, stampId: String) {
        proceedUnit {
            client.reaction().removeStampFromPost(
                RemoveStampFromPostRequest(postId = id.id(), stampId = stampId)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun shareComment(id: Identify) {
        doShareComment(id)
    }

    private suspend fun doShareComment(id: Identify) {
        proceedUnit {
            client.post().createPost(
                CreatePostRequest("").also { it.repostId = id.id() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unshareComment(id: Identify) {
        doUnshareComment(id)
    }

    private suspend fun doUnshareComment(id: Identify) {
        proceedUnit {
            client.post().deleteRepost(DeleteRepostRequest(referencePostId = id.id()))
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun deleteComment(id: Identify) {
        proceedUnit {
            client.post().deletePost(DeletePostRequest(id.id()))
        }
    }

    /**
     * {@inheritDoc}
     *
     * A mixi2 post is what it was when it was written; there is no edit.
     */
    override suspend fun editComment(id: Identify, req: CommentForm) {
        throw NotSupportedException("mixi2 has no post editing.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reportComment(id: Identify, comment: String?) {
        proceedUnit {
            client.moderation().reportPost(
                ReportPostRequest(
                    postId = id.id(),
                    reasonType = ReportReasonType.OTHER,
                    reasonContent = comment.orEmpty(),
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun bookmarkComment(id: Identify) {
        proceedUnit {
            client.post().createBookmark(CreateBookmarkRequest(id.id()))
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unbookmarkComment(id: Identify) {
        proceedUnit {
            client.post().deleteBookmark(DeleteBookmarkRequest(id.id()))
        }
    }

    /**
     * {@inheritDoc}
     *
     * mixi2 has no polls.
     */
    override suspend fun votePoll(id: Identify, choices: List<Int>) {
        throw NotSupportedException("mixi2 has no polls.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun commentContexts(id: Identify): Context {
        return fetchCommentContexts(id)
    }

    private suspend fun fetchCommentContexts(id: Identify): Context {
        val postId = id.id<String>()
        val ancestorPosts = proceed {
            client.post().getReplyAncestors(
                GetReplyAncestorsRequest(postId = postId, limit = DEFAULT_CONTEXT_COUNT)
            ).data.posts
        }.filter { it.postId != postId }

        val replyPosts = fetchAllReplies(postId).filter { it.postId != postId }

        val users = fetchPersonas(
            (ancestorPosts + replyPosts).flatMap { post ->
                listOfNotNull(post.personaId, post.referencePost?.personaId)
            }
        )

        return Context().also { context ->
            context.ancestors = ancestorPosts
                .map { Mixi2Mapper.comment(it, users, service()) }
                .sortedByDescending { it.createAt }
            context.descendants = replyPosts
                .map { Mixi2Mapper.comment(it, users, service()) }
                .sortedByDescending { it.createAt }
        }
    }

    /**
     * The context carries no paging parameter, so the replies are read to
     * their end; a repeated cursor ends the walk.
     */
    private suspend fun fetchAllReplies(postId: String): List<Post> {
        val posts = mutableListOf<Post>()
        var cursor: String? = null
        while (true) {
            val response = proceed {
                client.post().getReplies(
                    GetRepliesRequest(
                        postId = postId,
                        limit = DEFAULT_CONTEXT_COUNT,
                        cursor = cursor,
                    )
                ).data
            }
            posts.addAll(response.posts)

            val next = response.nextCursor
                .takeIf { response.hasNext && it.isNotBlank() }
                ?: break
            if (next == cursor) break
            cursor = next
        }
        return posts
    }

    // ============================================================== //
    // Space / Channel (Community)
    // ============================================================== //
    /**
     * {@inheritDoc}
     *
     * mixi2 has no container above a community; communities are the channels.
     */
    override suspend fun spaces(paging: Paging): Pageable<Space> {
        throw NotSupportedException("mixi2 has no container above a community.")
    }

    /**
     * {@inheritDoc}
     *
     * The participating communities are the channels the account can read.
     */
    override suspend fun channels(id: Identify, paging: Paging): Pageable<Channel> {
        val personaId = resolvePersonaId(id)
        val response = proceed {
            client.community().getParticipatingCommunities(
                GetParticipatingCommunitiesRequest().also {
                    it.personaId = personaId
                    it.limit = limit(paging)
                    it.cursor = cursor(paging)
                }
            ).data
        }
        return Mixi2Mapper.channels(
            communities = response.communities,
            service = service(),
            paging = paging,
            nextCursor = response.nextCursor?.ifBlank { null },
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return fetchCommunityTimeLine(id, paging)
    }

    /**
     * The community timeline answers with posts only. Its cursor names a post
     * (returning the older ones, exclusive), so the oldest post's id is the
     * cursor the next page asks for.
     */
    private suspend fun fetchCommunityTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        val posts = proceed {
            client.community().getCommunityTimeline(
                GetCommunityTimelineRequest(communityId = id.id<String>()).also {
                    it.untilCursorId = cursor(paging)
                    it.limit = limit(paging)
                }
            ).data.posts
        }
        return timeline(posts, paging, posts.lastOrNull()?.postId?.nextCursor(paging))
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
        val communityId = id.id<String>()
        val response = proceed {
            client.community().getParticipatingCommunityMembers(
                GetParticipatingCommunityMembersRequest(communityId = communityId).also {
                    it.limit = limit(paging)
                    it.cursor = cursor(paging)
                }
            ).data
        }

        val users = fetchPersonas(
            response.members
                .filter { it.persona == null }
                .mapNotNull { it.personaId.ifBlank { null } }
        )
        return Pageable<User>().also { p ->
            p.entities = response.members.mapNotNull { member ->
                member.persona?.let { Mixi2Mapper.user(it, service()) }
                    ?: users[member.personaId]
            }
            p.paging = Mixi2Paging.fromPaging(paging).also {
                it.nextCursor = response.cursor?.ifBlank { null }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * mixi2 communities are joined, not created; creation lives in the app's
     * community-administration RPCs, which the client does not model.
     */
    override suspend fun createList(name: String, description: String?): Channel {
        throw NotSupportedException("mixi2 lists are communities, joined rather than created.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun addUserToList(channel: Identify, user: Identify) {
        throw NotSupportedException("mixi2 membership is invited, not added.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun removeUserFromList(channel: Identify, user: Identify) {
        throw NotSupportedException("mixi2 membership is left rather than removed.")
    }

    // ============================================================== //
    // Message
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun messageThread(paging: Paging): Pageable<Thread> {
        val response = proceed {
            client.chat().getChatRooms(
                GetChatRoomsRequest().also {
                    it.limit = limit(paging)
                    it.untilMessageId = cursor(paging)
                }
            ).data
        }
        val users = fetchPersonas(
            response.rooms.flatMap { room -> room.members.map { it.personaId } }
        )
        val nextCursor = if (response.hasNext) {
            response.rooms.lastOrNull()?.message?.messageId
        } else {
            null
        }
        return Mixi2Mapper.threads(response.rooms, users, service(), paging, nextCursor)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun messageTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return fetchRoomMessages(id, paging)
    }

    private suspend fun fetchRoomMessages(id: Identify, paging: Paging): Pageable<Comment> {
        val roomId = id.id<String>()
        val response = proceed {
            client.chat().getChatRoomMessages(
                GetChatRoomMessagesRequest(roomId = roomId).also {
                    it.limit = limit(paging)
                    it.untilMessageId = cursor(paging)
                }
            ).data
        }
        val users = fetchPersonas(
            response.messages.flatMap { message ->
                listOfNotNull(
                    message.personaId,
                    message.post?.personaId,
                    message.post?.referencePost?.personaId,
                )
            }
        )
        val nextCursor = if (response.hasNext) {
            response.messages.lastOrNull()?.messageId
        } else {
            null
        }

        return Pageable<Comment>().also { p ->
            p.entities = response.messages.map { message ->
                Mixi2Mapper.chatComment(message, users, service())
            }
            p.paging = Mixi2Paging.fromPaging(paging).also { it.nextCursor = nextCursor }
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun postMessage(req: CommentForm) {
        doPostMessage(req)
    }

    private suspend fun doPostMessage(req: CommentForm) {
        if (req.poll != null) {
            throw NotSupportedException("mixi2 has no polls.")
        }
        if (req.quoteId != null) {
            throw NotSupportedException("A mixi2 room message cannot quote a post.")
        }
        val roomId = req.replyId?.value<String>()
            ?: throw NotSupportedException("A mixi2 message needs a room id (set replyId).")
        val mediaIds = req.images.map { uploadMedia(it) }
        proceedUnit {
            client.chat().sendMessageToRoom(
                SendMessageToRoomRequest(
                    roomId = roomId,
                    text = req.text,
                    mediaIds = mediaIds,
                )
            )
        }
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * {@inheritDoc}
     *
     * mixi2's web client polls; the MercuryService interface exposes no stream
     * socket, so a stream cannot be opened.
     */
    override suspend fun setHomeTimeLineStream(callback: EventCallback): Stream {
        throw NotSupportedException("mixi2 exposes no stream; poll the timeline instead.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        throw NotSupportedException("mixi2 exposes no stream; poll the notifications instead.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun setCommentUpdateStream(
        comments: List<Comment>,
        callback: EventCallback,
    ): CommentUpdateStream {
        throw NotSupportedException("mixi2 exposes no stream; poll the posts instead.")
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    private suspend fun timeline(
        posts: List<Post>,
        paging: Paging?,
        nextCursor: String?,
    ): Pageable<Comment> {
        val users = fetchPersonas(
            posts.flatMap { post ->
                listOfNotNull(post.personaId, post.referencePost?.personaId)
            }
        )
        return Mixi2Mapper.timeline(posts, users, service(), paging, nextCursor)
    }

    /**
     * The post-list timelines answer with posts only, so the oldest post's
     * time-series id is the cursor the next page asks for through
     * `untilCursorId`. A blank or repeated cursor would page forever, so it is
     * dropped.
     */
    private suspend fun fetchPostsTimeline(
        paging: Paging,
        fetcher: suspend () -> List<Post>,
    ): Pageable<Comment> {
        val posts = proceed { fetcher() }
        return timeline(posts, paging, posts.lastOrNull()?.timeSeriesId?.nextCursor(paging))
    }

    private fun String.nextCursor(paging: Paging): String? {
        return takeIf { it.isNotBlank() && it != cursor(paging) }
    }

    private suspend fun fetchComments(posts: List<Post>): List<Mixi2Comment> {
        val users = fetchPersonas(
            posts.flatMap { post ->
                listOfNotNull(post.personaId, post.referencePost?.personaId)
            }
        )
        return posts.map { Mixi2Mapper.comment(it, users, service()) }
    }

    /**
     * Resolve the personas of a page in one batch, remembering what was read.
     */
    private suspend fun fetchPersonas(ids: List<String>): Map<String, Mixi2User> {
        val missing = ids
            .filter { it.isNotBlank() }
            .distinct()
            .filter { !personaCache.containsKey(it) }

        if (missing.isNotEmpty()) {
            val personas = proceed {
                client.persona().getPersonas(GetPersonasRequest(missing)).data.personas
            }
            personas.forEach { persona ->
                personaCache[persona.personaId] = Mixi2Mapper.user(persona, service())
            }
        }

        return ids
            .filter { it.isNotBlank() }
            .distinct()
            .mapNotNull { id -> personaCache[id]?.let { id to it } }
            .toMap()
    }

    private suspend fun fetchPersonaByName(name: String): Persona {
        return proceed {
            client.persona().getPersonaByName(GetPersonaByNameRequest(name)).data.persona
        } ?: throw NotFoundException(null, "The mixi2 persona was not found.", null)
    }

    /**
     * A persona is addressed by id; a name is resolved first.
     */
    private suspend fun resolvePersonaId(id: Identify): String {
        if (id is Mixi2User && id.personaId.isNotEmpty()) {
            return id.personaId
        }
        val value = id.id<String>()
        if (id is Mixi2User && id.handle.isNotBlank()) {
            return fetchPersonaByName(id.handle).personaId
        }
        return if (isPersonaName(value)) fetchPersonaByName(value).personaId else value
    }

    private fun isPersonaName(value: String): Boolean {
        return value.isNotEmpty() &&
            !UUID_REGEX.matches(value) &&
            value.all { it.isLetterOrDigit() || it == '_' }
    }

    private suspend fun requireActivePersona(id: Identify, message: String) {
        val expected = activePersonaId ?: fetchUserMe().personaId
        if (resolvePersonaId(id) != expected) {
            throw NotSupportedException(message)
        }
    }

    private suspend fun uploadMedia(
        form: MediaForm,
        communityId: String? = null,
    ): String {
        val mimeType = mediaMimeType(form.name)
        val media = proceed {
            client.media().uploadMedia(
                UploadMediaRequest(
                    mimeType = mimeType,
                    data = form.data,
                    category = mediaCategory(mimeType),
                    communityId = communityId,
                    description = form.description,
                )
            ).data
        }
        return media.mediaId
    }

    private fun mediaMimeType(name: String?): String {
        return when (name?.substringAfterLast('.', "")?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            else -> "image/png"
        }
    }

    private fun mediaCategory(mimeType: String): MediaCategory {
        return if (mimeType.startsWith("video/")) {
            MediaCategory.POST_VIDEO
        } else {
            MediaCategory.POST_IMAGE
        }
    }

    private fun segments(url: String): List<String> {
        return url.substringBefore('?')
            .substringBefore('#')
            .trimEnd('/')
            .split('/')
            .filter { it.isNotEmpty() }
    }

    private fun cursor(paging: Paging?): String? {
        return (paging as? Mixi2Paging)?.cursor
    }

    private fun limit(paging: Paging?): Int {
        return paging?.count ?: DEFAULT_COUNT
    }

    private fun service(): Service {
        return account.service
    }

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Mixi2,
            statusExtractor = { exception -> (exception as? Mixi2WebException)?.status },
            bodyExtractor = { exception ->
                (exception as? Mixi2WebException)?.responseBody?.decodeToString()
            },
            runner = runner,
        )
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Mixi2,
            statusExtractor = { exception -> (exception as? Mixi2WebException)?.status },
            bodyExtractor = { exception ->
                (exception as? Mixi2WebException)?.responseBody?.decodeToString()
            },
            runner = runner,
        )
    }
}
