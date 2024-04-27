package work.socialhub.planetlink.tumblr.action

import work.socialhub.ktumblr.TumblrException
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.tumblr.define.TumblrIconSize

class TumblrAction(
    account: Account,
    val auth: TumblrAuth,
) : AccountActionImpl(account) {

    /** AvatarIcon Url Mapper */
    private val avaterMapper = mutableMapOf<String, String>()

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
        return TumblrMapper.getAvatarUrl(host, size)
    }

    /**
     * {@inheritDoc}
     */
    override fun userMe(): User {
        return proceed {
            val user = auth.accessor.user().user()

            // アイコンキャッシュから取得
            val host: String = TumblrMapper.getUserIdentify(user.getBlogs())
            val result: User = TumblrMapper.user(user, service)

            // ホスト情報が存在
            if (host != null) {
                // 投稿が一つでも存在すればその投稿情報を取得

                val posts: List<Post> = auth.getAccessor().blogPosts(host, limit1())

                if ((posts != null) && (posts.size > 0)) {
                    val trails: Map<String, Trail> = TumblrMapper.getTrailMap(posts)
                    val cover: User = TumblrMapper.user(posts[0], trails, service)
                    TumblrMapper.margeUser(result, cover)
                }
            }
            me = result
            result
        }
    }

    /**
     * {@inheritDoc}
     */
    fun getUser(id: Identify): User {
        return proceed({
            val pool: ExecutorService = Executors.newCachedThreadPool()


            // デフォルトのユーザー情報の取得
            val resultFuture: java.util.concurrent.Future<User> = pool.submit(java.util.concurrent.Callable<T> {
                val blog: Blog = auth.getAccessor().blogInfo(id.getId() as String)
                TumblrMapper.user(blog, service)
            })

            // カバー情報を取得するためのリクエスト
            val coverFuture: java.util.concurrent.Future<User> = pool.submit(java.util.concurrent.Callable<T> {
                val posts: List<Post> = auth.getAccessor() //
                    .blogPosts(id.getId() as String, limit1())
                if ((posts != null) && (posts.size > 0)) {
                    val trails: Map<String, Trail> = TumblrMapper.getTrailMap(posts)
                    return@submit TumblrMapper.user(posts[0], trails, service)
                }
                null
            })

            val resultUser: User = resultFuture.get()
            val coverUser: User = coverFuture.get()

            TumblrMapper.margeUser(resultUser, coverUser)
            resultUser
        })
    }

    /**
     * {@inheritDoc}
     */
    fun followUser(id: Identify) {
        proceed({
            auth.getAccessor().follow(id.getId() as String)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun unfollowUser(id: Identify) {
        proceed({
            auth.getAccessor().unfollow(id.getId() as String)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getRelationship(id: Identify): Relationship {
        return proceed({
            // オブジェクトに格納済みなので返却
            if (id is TumblrUser) {
                return@proceed (id as TumblrUser).getRelationship()
            }

            // ユーザーの一部なのでそれを返却
            val user: User = getUser(id)
            if (user is TumblrUser) {
                return@proceed (user as TumblrUser).getRelationship()
            }
            throw java.lang.IllegalStateException()
        })
    }

    // ============================================================== //
    // User
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getFollowingUsers(id: Identify?, paging: Paging?): Pageable<User> {
        return proceed({

            // TODO: 自分のアカウントのブログであるかどうか？
            val params: Map<String, Any> = getPagingParams(paging)
            val blogs: List<Blog> = auth.getAccessor().userFollowing(params)
            TumblrMapper.usersByBlogs(blogs, service, paging)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getFollowerUsers(id: Identify, paging: Paging?): Pageable<User> {
        return proceed({

            // TODO: 自分のアカウントのブログであるかどうか？
            val params: Map<String, Any> = getPagingParams(paging)
            val users: List<com.tumblr.jumblr.types.User> = auth.getAccessor() //
                .blogFollowers(id.getId() as String, params)
            TumblrMapper.users(users, service, paging)
        })
    }

    // ============================================================== //
    // TimeLine
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getHomeTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({

            val params = getPagingParams(paging)
            setPagingOptions(params)

            val posts: List<Post> = auth.getAccessor().userDashboard(params)
            TumblrMapper.timeLine(posts, service, paging)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getUserCommentTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({

            val params = getPagingParams(paging)
            setPagingOptions(params)

            val posts: List<Post> = auth.getAccessor().blogPosts(id.getId() as String, params)
            TumblrMapper.timeLine(posts, service, paging)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getUserLikeTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({

            val params: Map<String, Any> = getPagingParams(paging)

            val posts: List<Post> = auth.getAccessor().blogLikes(id.getId() as String, params)
            TumblrMapper.timeLine(posts, service, paging)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getSearchTimeLine(query: String?, paging: Paging?): Pageable<Comment> {
        return proceed({

            val params: Map<String, Any> = getPagingParams(paging)

            val posts: List<Post> = auth.getAccessor().tagged(query, params)
            TumblrMapper.timeLine(posts, service, paging)
        })
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun postComment(req: CommentForm) {
        proceed({
            val me: User = getUserMeWithCache()
            val post: Post

            if (req.getImages() != null && !req.getImages().isEmpty()) {
                // PhotoPost

                val photoPost: PhotoPost = auth.getAccessor() //
                    .newPost(me.getId() as String, PhotoPost::class.java)

                for (media in req.getImages()) {
                    val file: Photo.ByteFile = ByteFile()
                    file.setBytes(media.getData())
                    file.setName(media.getName())
                    val photo: Photo = Photo(file)
                    photoPost.addPhoto(photo)
                }

                photoPost.setCaption(req.getText())
                post = photoPost
            } else {
                // TextPost

                val textPost: TextPost = auth.getAccessor() //
                    .newPost(me.getId() as String, TextPost::class.java)

                textPost.setBody(req.getText())
                post = textPost
            }

            // Save
            post.save()
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getComment(id: Identify): Comment {
        return proceed({
            if (id is TupleIdentify) {
                val tuple: TupleIdentify = (id as TupleIdentify)

                val post: Post = auth.getAccessor().blogPost( //
                    tuple.getSubId() as String, tuple.getId() as Long
                )

                val trails: Map<String, Trail> = TumblrMapper.getTrailMap(listOf(post))
                return@proceed TumblrMapper.comment(post, trails, service)
            } else {
                throw NotSupportedException("TupleIdentify required.")
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    fun likeComment(id: Identify) {
        proceed({
            if (id is TumblrComment) {
                val key: String = (id as TumblrComment).getReblogKey()
                auth.getAccessor().like(id.getId() as Long, key)
            } else {
                throw NotSupportedException("TumblrComment (id and reblog key only) required.")
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    fun unlikeComment(id: Identify) {
        proceed({
            if (id is TumblrComment) {
                val key: String = (id as TumblrComment).getReblogKey()
                auth.getAccessor().unlike(id.getId() as Long, key)
            } else {
                throw NotSupportedException("TumblrComment (id and reblog key only) required.")
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    fun shareComment(id: Identify) {
        proceed({
            if (id is TumblrComment) {
                val key: String = (id as TumblrComment).getReblogKey()
                val blog: String = getUserMeWithCache().getScreenName()
                auth.getAccessor().postReblog(blog, id.getId() as Long, key)
            } else {
                throw NotSupportedException("TumblrComment (id, blogName reblog key only) required.")
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    fun unshareComment(id: Identify?) {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    fun reactionComment(id: Identify, reaction: String?) {
        if (reaction != null && !reaction.isEmpty()) {
            val type = reaction.lowercase(Locale.getDefault())

            if (TumblrReactionType.Like.getCode().contains(type)) {
                likeComment(id)
                return
            }
            if (TumblrReactionType.Reblog.getCode().contains(type)) {
                retweetComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    fun unreactionComment(id: Identify, reaction: String?) {
        if (reaction != null && !reaction.isEmpty()) {
            val type = reaction.lowercase(Locale.getDefault())

            if (TumblrReactionType.Like.getCode().contains(type)) {
                unlikeComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    fun deleteComment(id: Identify) {
        proceed({
            if (id is TumblrComment) {
                val blog: String = getUserMeWithCache().getScreenName()
                auth.getAccessor().postDelete(blog, id.getId() as Long)
            } else {
                throw NotSupportedException("TumblrComment (id, blog n ame only) required.")
            }
        })
    }

    // ============================================================== //
    // Paging
    // ============================================================== //
    private fun getPagingParams(paging: Paging?): MutableMap<String, Any> {
        val params: MutableMap<String, Any> = java.util.HashMap<String, Any>()

        if (paging != null) {
            if (paging.getCount() != null) {
                params["limit"] = paging.getCount()
            }

            if (paging is TumblrPaging) {
                val pg: TumblrPaging = paging as TumblrPaging

                if (pg.getSinceId() != null) {
                    params["since_id"] = pg.getSinceId()
                }
                if (pg.getOffset() != null) {
                    params["offset"] = pg.getOffset()
                }
            }
        }

        return params
    }

    // ============================================================== //
    // Request Samples
    // ============================================================== //
    private fun limit1(): Map<String, Any> {
        val params: MutableMap<String, Any> = java.util.HashMap<String, Any>()
        params["limit"] = 1
        return params
    }

    private fun setPagingOptions(params: MutableMap<String, Any>) {
        params["reblog_info"] = true
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
