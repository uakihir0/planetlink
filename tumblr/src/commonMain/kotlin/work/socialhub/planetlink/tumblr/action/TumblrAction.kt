package work.socialhub.planetlink.tumblr.action

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
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.support.TupleIdentify
import work.socialhub.planetlink.tumblr.define.TumblrIconSize
import work.socialhub.planetlink.tumblr.define.TumblrReactionType
import work.socialhub.planetlink.tumblr.model.TumblrComment
import work.socialhub.planetlink.tumblr.model.TumblrPaging
import work.socialhub.planetlink.tumblr.model.TumblrUser

class TumblrAction(
    account: Account,
    val auth: TumblrAuth,
) : AccountActionImpl(account) {

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
    override fun userMe(): User {
        return proceed {
            val user = validateToken {
                auth.accessor.user().user()
            }.data.response?.user
            checkNotNull(user)

            // アイコンキャッシュから取得
            val host = TumblrMapper.userIdentify(user.blogs!!)
            val result = TumblrMapper.user(user, service())

            // 投稿が一つでも存在すればその投稿情報を取得
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

            result.also {
                me = it
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun user(
        id: Identify
    ): User {
        return proceed {
            // TODO: 並列実行

            // デフォルトのユーザー情報の取得
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

            // カバー情報を取得するためのリクエスト
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

                // カバーを発見した場合はその情報を譲渡
                TumblrMapper.margeUser(resultUser, coverUser)
            }

            resultUser
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun followUser(
        id: Identify
    ) {
        proceedUnit {
            validateToken {
                auth.accessor.user().follow(
                    UserFollowRequest().also {
                        it.url = blogUrl(id.id<String>())
                    }
                )
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unfollowUser(
        id: Identify
    ) {
        proceedUnit {
            validateToken {
                auth.accessor.user().unfollow(
                    UserUnfollowRequest().also {
                        it.url = blogUrl(id.id<String>())
                    }
                )
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun relationship(
        id: Identify
    ): Relationship {
        return proceed {
            // オブジェクトに格納済みなので返却
            if (id is TumblrUser) {
                return@proceed id.relationship!!
            }

            // ユーザーの一部なのでそれを返却
            val user = user(id)
            if (user is TumblrUser) {
                return@proceed user.relationship!!
            }
            throw IllegalStateException()
        }
    }

    // ============================================================== //
    // User
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun followingUsers(
        id: Identify,
        paging: Paging
    ): Pageable<User> {
        return proceed {
            // TODO: 自分のアカウントのブログであるかどうか？

            val blogs = validateToken {
                auth.accessor.user().userFollowing(
                    UserFollowingRequest().also {
                        it.limit = limit(paging)
                        it.offset = offset(paging)
                    }).data.response?.blogs
            }

            TumblrMapper.usersByBlogs(
                checkNotNull(blogs),
                service(),
                paging
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun followerUsers(
        id: Identify,
        paging: Paging
    ): Pageable<User> {
        return proceed {
            // TODO: 自分のアカウントのブログであるかどうか？

            val users = validateToken {
                auth.accessor.blog().blogFollowers(
                    BlogFollowersRequest().also {
                        it.blogName = id.id<String>()
                        it.limit = limit(paging)
                    }).data.response?.users
            }

            TumblrMapper.followerUsers(
                checkNotNull(users),
                service(),
                paging
            )
        }
    }

    // ============================================================== //
    // TimeLine
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun homeTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
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

            TumblrMapper.timeLine(
                checkNotNull(posts),
                service(),
                paging
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userCommentTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
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

            TumblrMapper.timeLine(
                checkNotNull(posts),
                service(),
                paging
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userLikeTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val posts = validateToken {
                auth.accessor.blog().blogLikes(
                    BlogLikesRequest().also {
                        it.blogName = id.id<String>()
                        it.limit = limit(paging)
                        it.offset = offset(paging)
                    }).data.response?.likedPosts
            }

            TumblrMapper.timeLine(
                checkNotNull(posts),
                service(),
                paging
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun searchTimeLine(
        query: String,
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val posts = validateToken {
                auth.accessor.tagged().tagged(
                    TaggedRequest().also {
                        it.tag = query
                        it.limit = limit(paging)
                    }).data.response
            }

            TumblrMapper.timeLine(
                checkNotNull(posts),
                service(),
                paging
            )
        }
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun postComment(
        req: CommentForm
    ) {
        proceedUnit {
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
    }

    /**
     * {@inheritDoc}
     */
    override fun comment(
        id: Identify
    ): Comment {
        return proceed {
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

                TumblrMapper.comment(
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
    }

    /**
     * {@inheritDoc}
     */
    override fun likeComment(
        id: Identify
    ) {
        proceedUnit {
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
    }

    /**
     * {@inheritDoc}
     */
    override fun unlikeComment(
        id: Identify
    ) {
        proceedUnit {
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
    }

    /**
     * {@inheritDoc}
     */
    override fun shareComment(
        id: Identify
    ) {
        proceedUnit {
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
    }

    /**
     * {@inheritDoc}
     */
    override fun unshareComment(
        id: Identify
    ) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override fun reactionComment(
        id: Identify,
        reaction: String
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (TumblrReactionType.Like.codes.contains(type)) {
                likeComment(id)
                return
            }
            if (TumblrReactionType.Reblog.codes.contains(type)) {
                shareComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override fun unreactionComment(
        id: Identify,
        reaction: String
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (TumblrReactionType.Like.codes.contains(type)) {
                unlikeComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override fun deleteComment(
        id: Identify
    ) {
        proceed {
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
                    "TumblrComment (id, blog n ame only) required."
                )
            }
        }
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

    private fun <T> validateToken(
        func: () -> T
    ): T {
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

                // トークン類を取得した値で更新
                auth.accessToken = refresh.data.accessToken
                auth.refreshToken = refresh.data.refreshToken
                return func()
            }

            throw e
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
    private fun <T> proceed(runner: () -> T): T {
        try {
            return runner()
        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    private fun proceedUnit(runner: () -> Unit) {
        try {
            runner()
        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    private fun handleException(
        e: Exception
    ): SocialHubException {

        if ((e is TumblrException) && (e.message != null)) {
            return SocialHubException(e.message, e)
            // TODO: エラーメッセージが設定されているエラーである場合
        }
        return SocialHubException(e)
    }
}
