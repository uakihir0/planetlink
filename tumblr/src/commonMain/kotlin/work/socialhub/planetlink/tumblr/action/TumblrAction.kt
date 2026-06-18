package work.socialhub.planetlink.tumblr.action

import kotlin.coroutines.cancellation.CancellationException
import work.socialhub.ktumblr.TumblrException
import work.socialhub.ktumblr.api.request.FileRequest
import work.socialhub.ktumblr.api.request.auth.AuthOAuth2TokenRefreshRequest
import work.socialhub.ktumblr.api.request.blog.BlogFollowersRequest
import work.socialhub.ktumblr.api.request.blog.BlogInfoRequest
import work.socialhub.ktumblr.api.request.blog.BlogLikesRequest
import work.socialhub.ktumblr.api.request.blog.BlogPostsRequest
import work.socialhub.ktumblr.api.request.blog.post.BlogDeleteRequest
import work.socialhub.ktumblr.api.request.blog.post.BlogPhotoPostRequest
import work.socialhub.ktumblr.api.request.blog.post.BlogPostRequest
import work.socialhub.ktumblr.api.request.blog.post.BlogReblogRequest
import work.socialhub.ktumblr.api.request.blog.post.BlogTextPostRequest
import work.socialhub.ktumblr.api.request.tagged.TaggedRequest
import work.socialhub.ktumblr.api.request.user.UserDashboardRequest
import work.socialhub.ktumblr.api.request.user.UserFollowRequest
import work.socialhub.ktumblr.api.request.user.UserFollowingRequest
import work.socialhub.ktumblr.api.request.user.UserLikeRequest
import work.socialhub.ktumblr.api.request.user.UserUnfollowRequest
import work.socialhub.ktumblr.api.request.user.UserUnlikeRequest
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.utils.ExceptionHandler
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.support.TupleIdentify
import work.socialhub.planetlink.tumblr.define.TumblrIconSize
import work.socialhub.planetlink.tumblr.define.TumblrReactionType
import work.socialhub.planetlink.tumblr.model.TumblrComment
import work.socialhub.planetlink.tumblr.model.TumblrPaging
import work.socialhub.planetlink.tumblr.model.TumblrUser
import kotlin.js.JsExport

@JsExport
class TumblrAction(
    account: Account,
    val auth: TumblrAuth,
) : AccountActionImpl(account) {

    companion object {
        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.FollowUser,
                SocialActionType.UnfollowUser,
                SocialActionType.GetRelationship,
                SocialActionType.GetComment,
                SocialActionType.GetContext,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.LikeComment,
                SocialActionType.UnlikeComment,
                SocialActionType.ShareComment,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.UserLikeTimeLine,
                TimeLineActionType.UserMediaTimeLine,
                TimeLineActionType.SearchTimeLine,

                UsersActionType.GetFollowingUsers,
                UsersActionType.GetFollowerUsers,
            )
        )
    }

    override fun capabilities(): Capabilities = CAPABILITIES

    // ============================================================== //
    // Account
    // ============================================================== //
    /**
     * Get User's Avatar Icon Url
     * アバター画像 URL を取得
     */
    fun userAvatar(
        host: String
    ): String {
        val size: TumblrIconSize = TumblrIconSize.S512
        return TumblrMapper.avatarUrl(host, size)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userMe(): User {
        return fetchUserMe()
    }

    /**
     * Overrides the base `userMeWithCache()` and routes both it and `userMe()`
     * through this private function to avoid the Kotlin/JS yield* crash caused by
     * the unwired virtual suspend bridge for base→abstract `userMe()` delegation.
     * See AGENTS.md "Kotlin/JS yield* Bug".
     */
    private suspend fun fetchUserMe(): User {
        val user = validateToken {
            auth.accessor.user().user()
        }.data.response?.user
        checkNotNull(user)

        val host = TumblrMapper.userIdentify(user.blogs!!)
        val result = TumblrMapper.user(user, service())

        val posts = validateToken {
            auth.accessor.blog().blogPosts(
                BlogPostsRequest().also {
                    it.blogName = host
                    it.limit = 1
                }).data.response?.posts
        }

        if (!posts.isNullOrEmpty()) {
            val trails = TumblrMapper.trailMap(posts)
            val cover = TumblrMapper.user(posts[0], trails, service())
            TumblrMapper.margeUser(result, cover)
        }

        me = result
        return result
    }

    override suspend fun userMeWithCache(): User {
        return me ?: fetchUserMe()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun user(
        id: Identify
    ): User {
        return fetchUser(id)
    }

    // Free-standing impl so same-class callers (user(url), relationship) don't route
    // through the unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun fetchUser(id: Identify): User {
        val blog = validateToken {
            auth.accessor.blog().blogInfo(
                BlogInfoRequest().also {
                    it.blogName = id.id<String>()
                }).data.response?.blog
        }

        val resultUser = TumblrMapper.user(
            checkNotNull(blog),
            service()
        )

        val posts = validateToken {
            auth.accessor.blog().blogPosts(
                BlogPostsRequest().also {
                    it.blogName = id.id<String>()
                    it.limit = 1
                }).data.response?.posts
        }

        if (!posts.isNullOrEmpty()) {
            val trails = TumblrMapper.trailMap(posts)
            val coverUser = TumblrMapper.user(posts[0], trails, service())
            TumblrMapper.margeUser(resultUser, coverUser)
        }

        return resultUser
    }

    /**
     * {@inheritDoc}
     * https://www.tumblr.com/uakihiro
     */
    override suspend fun user(
        url: String
    ): User {
        val name = url.split("/").last()
        return fetchUser(Identify(service(), ID(name)))
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun followUser(
        id: Identify
    ) {
        validateToken {
            auth.accessor.user().follow(
                UserFollowRequest().also {
                    it.url = blogUrl(id.id<String>())
                }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unfollowUser(
        id: Identify
    ) {
        validateToken {
            auth.accessor.user().unfollow(
                UserUnfollowRequest().also {
                    it.url = blogUrl(id.id<String>())
                }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun muteUser(
        id: Identify
    ) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unmuteUser(
        id: Identify
    ) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun blockUser(
        id: Identify
    ) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unblockUser(
        id: Identify
    ) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun relationship(
        id: Identify
    ): Relationship {
        // オブジェクトに格納済みなので返却
        if (id is TumblrUser) {
            return id.relationship!!
        }

        // ユーザーの一部なのでそれを返却
        val user = fetchUser(id)
        if (user is TumblrUser) {
            return user.relationship!!
        }
        throw IllegalStateException()
    }

    // ============================================================== //
    // User
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun followingUsers(
        id: Identify,
        paging: Paging
    ): Pageable<User> {
        val blogs = validateToken {
            auth.accessor.user().userFollowing(
                UserFollowingRequest().also {
                    it.limit = limit(paging)
                    it.offset = offset(paging)
                }).data.response?.blogs
        }

        return TumblrMapper.usersByBlogs(
            checkNotNull(blogs),
            service(),
            paging
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun followerUsers(
        id: Identify,
        paging: Paging
    ): Pageable<User> {
        val users = validateToken {
            auth.accessor.blog().blogFollowers(
                BlogFollowersRequest().also {
                    it.blogName = id.id<String>()
                    it.limit = limit(paging)
                }).data.response?.users
        }

        return TumblrMapper.followerUsers(
            checkNotNull(users),
            service(),
            paging
        )
    }

    // ============================================================== //
    // TimeLine
    // ============================================================== //

    /**
     * {@inheritDoc}
     */
    override suspend fun searchUsers(
        query: String,
        paging: Paging
    ): Pageable<User> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun mentionTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun homeTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        val posts = validateToken {
            auth.accessor.user().userDashboard(
                UserDashboardRequest().also {
                    it.limit = limit(paging)
                    it.offset = offset(paging)
                    it.sinceId = sinceId(paging)?.toInt() // FIXME
                    it.reblogInfo = true
                    it.notesInfo = true
                }).data.response?.posts
        }

        return TumblrMapper.timeLine(
            checkNotNull(posts),
            service(),
            paging
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userCommentTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        val posts = validateToken {
            auth.accessor.blog().blogPosts(
                BlogPostsRequest().also {
                    it.blogName = id.id<String>()
                    it.limit = limit(paging)
                    it.offset = offset(paging)
                    it.reblogInfo = true
                    it.notesInfo = true
                }).data.response?.posts
        }

        return TumblrMapper.timeLine(
            checkNotNull(posts),
            service(),
            paging
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userLikeTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        val posts = validateToken {
            auth.accessor.blog().blogLikes(
                BlogLikesRequest().also {
                    it.blogName = id.id<String>()
                    it.limit = limit(paging)
                    it.offset = offset(paging)
                }).data.response?.likedPosts
        }

        return TumblrMapper.timeLine(
            checkNotNull(posts),
            service(),
            paging
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userMediaTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        val posts = validateToken {
            auth.accessor.blog().blogPosts(
                BlogPostsRequest().also {
                    it.blogName = id.id<String>()
                    it.limit = limit(paging)
                    it.offset = offset(paging)
                    it.reblogInfo = true
                    it.notesInfo = true
                }).data.response?.posts
        }

        return TumblrMapper.timeLine(
            checkNotNull(posts),
            service(),
            paging
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun searchTimeLine(
        query: String,
        paging: Paging
    ): Pageable<Comment> {
        val posts = validateToken {
            auth.accessor.tagged().tagged(
                TaggedRequest().also {
                    it.tag = query
                    it.limit = limit(paging)
                }).data.response
        }

        return TumblrMapper.timeLine(
            checkNotNull(posts),
            service(),
            paging
        )
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun postComment(
        req: CommentForm
    ) {
        val me = userMeWithCache()
        val post: BlogPostRequest

        if (req.images.isNotEmpty()) {

            // PhotoPost
            post = BlogPhotoPostRequest().also { r ->
                r.data = req.images.map { img ->
                    FileRequest(
                        data = img.data,
                        name = img.name,
                    )
                }.toTypedArray()
                r.caption = req.text
                r.type = "photo"
            }

        } else {

            // TextPost
            post = BlogTextPostRequest().also { r ->
                r.body = req.text
                r.type = "text"
            }
        }

        post.blogName = me.id<String>()

        validateToken {
            auth.accessor.blog()
                .postCreate(post)
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun comment(
        id: Identify
    ): Comment {
        if (id is TupleIdentify) {
            val posts = validateToken {
                auth.accessor.blog().blogPosts(
                    BlogPostsRequest().also {
                        it.blogName = id.subId?.value<String>()
                        it.id = id.id?.value<String>()?.toInt() // FIXME
                        it.limit = 1
                    }).data.response?.posts
            }

            checkNotNull(posts)
            val trails = TumblrMapper.trailMap(posts)

            return TumblrMapper.comment(
                posts[0],
                trails,
                service()
            )
        } else {
            throw NotSupportedException(
                "TupleIdentify required."
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun comment(
        url: String
    ): Comment {
        val blogName = url.split("/")[3]
        val postId = url.split("/").last()
        val posts = validateToken {
            auth.accessor.blog().blogPosts(
                BlogPostsRequest().also {
                    it.blogName = blogName
                    it.id = postId.toInt() // FIXME
                    it.limit = 1
                }).data.response?.posts
        }
        checkNotNull(posts)
        val trails = TumblrMapper.trailMap(posts)
        return TumblrMapper.comment(
            posts[0],
            trails,
            service()
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun likeComment(
        id: Identify
    ) {
        doLikeComment(id)
    }

    // Free-standing impls so same-class callers (reactionComment) don't route
    // through the unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doLikeComment(id: Identify) {
        if (id is TumblrComment) {
            validateToken {
                auth.accessor.user().like(
                    UserLikeRequest().also {
                        it.id = id.id<String>()
                        it.reblogKey = id.reblogKey
                    })
            }
        } else {
            throw NotSupportedException(
                "TumblrComment (id and reblog key only) required."
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unlikeComment(
        id: Identify
    ) {
        doUnlikeComment(id)
    }

    private suspend fun doUnlikeComment(id: Identify) {
        if (id is TumblrComment) {
            validateToken {
                auth.accessor.user().unlike(
                    UserUnlikeRequest().also {
                        it.id = id.id<String>()
                        it.reblogKey = id.reblogKey
                    })
            }
        } else {
            throw NotSupportedException(
                "TumblrComment (id and reblog key only) required."
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun shareComment(
        id: Identify
    ) {
        doShareComment(id)
    }

    private suspend fun doShareComment(id: Identify) {
        if (id is TumblrComment) {
            val blog = userMeWithCache()
            validateToken {
                auth.accessor.blog().postReblog(
                    BlogReblogRequest().also {
                        it.id = id.id<String>()
                        it.reblogKey = id.reblogKey
                        it.blogName = blog.id<String>()
                    })
            }
        } else {
            throw NotSupportedException(
                "TumblrComment (id and reblog key only) required."
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unshareComment(
        id: Identify
    ) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reactionComment(
        id: Identify,
        reaction: String
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (TumblrReactionType.Like.codes.contains(type)) {
                doLikeComment(id)
                return
            }
            if (TumblrReactionType.Reblog.codes.contains(type)) {
                doShareComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unreactionComment(
        id: Identify,
        reaction: String
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (TumblrReactionType.Like.codes.contains(type)) {
                doUnlikeComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun deleteComment(
        id: Identify
    ) {
        if (id is TumblrComment) {
            val blog = userMeWithCache()
            validateToken {
                auth.accessor.blog().postDelete(
                    BlogDeleteRequest().also {
                        it.blogName = blog.id<String>()
                        it.id = id.id<String>()
                    })
            }
        } else {
            throw NotSupportedException(
                "TumblrComment (id, blog name only) required."
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun commentContexts(
        id: Identify
    ): work.socialhub.planetlink.model.Context {
        val context = work.socialhub.planetlink.model.Context()

        if (id is TupleIdentify) {
            val posts = validateToken {
                auth.accessor.blog().blogPosts(
                    BlogPostsRequest().also {
                        it.blogName = id.subId?.value<String>()
                        it.id = id.id?.value<String>()?.toInt() // FIXME
                        it.limit = 1
                    }).data.response?.posts
            }

            if (!posts.isNullOrEmpty()) {
                val post = posts[0]

                val ancestors = mutableListOf<work.socialhub.planetlink.model.Comment>()
                var currentParentUrl = post.parentPostUrl
                while (currentParentUrl != null) {
                    val parentParts = currentParentUrl.trim('/').split('/')
                    if (parentParts.size >= 2) {
                        val parentBlog = parentParts[0]
                        val parentId = parentParts.drop(1).lastOrNull()
                        if (parentId != null) {
                            val parentPosts = validateToken {
                                auth.accessor.blog().blogPosts(
                                    BlogPostsRequest().also {
                                        it.blogName = parentBlog
                                        it.id = parentId.toInt() // FIXME
                                        it.limit = 1
                                    }).data.response?.posts
                            }
                            if (!parentPosts.isNullOrEmpty()) {
                                val parentTrails = TumblrMapper.trailMap(parentPosts)
                                ancestors.add(0, TumblrMapper.comment(
                                    parentPosts[0],
                                    parentTrails,
                                    service()
                                ))
                                currentParentUrl = parentPosts[0].parentPostUrl
                            } else {
                                currentParentUrl = null
                            }
                        } else {
                            currentParentUrl = null
                        }
                    } else {
                        currentParentUrl = null
                    }
                }
                context.ancestors = ancestors
                context.descendants = listOf()
            }
        }

        return context
    }

    // ============================================================== //
    // Channel (List) API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun channels(
        id: Identify,
        paging: Paging
    ): Pageable<work.socialhub.planetlink.model.Channel> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun channelTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun channelUsers(
        id: Identify,
        paging: Paging
    ): Pageable<User> {
        throw NotSupportedException()
    }

    // ============================================================== //
    // Message API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun messageThread(
        paging: Paging
    ): Pageable<Thread> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun messageTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun postMessage(
        req: CommentForm
    ) {
        throw NotSupportedException()
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun setHomeTimeLineStream(
        callback: work.socialhub.planetlink.action.callback.EventCallback
    ): work.socialhub.planetlink.model.Stream {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun setNotificationStream(
        callback: work.socialhub.planetlink.action.callback.EventCallback
    ): work.socialhub.planetlink.model.Stream {
        throw NotSupportedException()
    }

    // ============================================================== //
    // Paging
    // ============================================================== //
    private fun limit(paging: Paging): Int? = paging.count

    private fun offset(paging: Paging): Int? {
        return if (paging is TumblrPaging) {
            paging.offset
        } else null
    }

    private fun sinceId(paging: Paging): String? {
        return if (paging is TumblrPaging) {
            paging.sinceId
        } else null
    }


    // ============================================================== //
    // Refresh
    // ============================================================== //

    private suspend fun <T> validateToken(
        func: suspend () -> T
    ): T {
        try {
            try {
                return func()
            } catch (e: TumblrException) {
                if (e.status == 401) {
                    val refresh = auth.accessor.auth().oAuth2TokenRefresh(
                        AuthOAuth2TokenRefreshRequest().also {
                            it.clientId = auth.consumerKey
                            it.clientSecret = auth.consumerSecret
                            it.refreshToken = auth.refreshToken
                        }
                    )

                    auth.accessToken = refresh.data.accessToken
                    auth.refreshToken = refresh.data.refreshToken
                    auth.tokenRefreshCallback(auth)
                    return func()
                }
                throw e
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocialHubException) {
            if (e.serviceType == null) e.serviceType = ServiceType.Tumblr
            throw e
        } catch (e: Exception) {
            throw ExceptionHandler.classify(
                e = e,
                serviceType = ServiceType.Tumblr,
                statusCode = (e as? TumblrException)?.status,
                responseBody = (e as? TumblrException)?.body,
            )
        }
    }


    // ============================================================== //
    // Support
    // ============================================================== //

    private fun blogUrl(
        blogName: String,
    ): String {
        return if (blogName.contains(".")) blogName
        else "$blogName.tumblr.com"
    }

    private fun service(): Service {
        return account.service
    }


    // ============================================================== //
    // Utils
    // ============================================================== //
    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Tumblr,
            statusExtractor = { e -> (e as? TumblrException)?.status },
            bodyExtractor = { e -> (e as? TumblrException)?.body },
            runner = runner,
        )
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Tumblr,
            statusExtractor = { e -> (e as? TumblrException)?.status },
            bodyExtractor = { e -> (e as? TumblrException)?.body },
            runner = runner,
        )
    }
}
