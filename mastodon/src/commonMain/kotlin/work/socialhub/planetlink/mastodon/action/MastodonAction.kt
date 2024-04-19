package work.socialhub.planetlink.mastodon.action


import work.socialhub.kmastodon.MastodonException
import work.socialhub.kmastodon.api.request.Page
import work.socialhub.kmastodon.api.request.Range
import work.socialhub.kmastodon.api.request.accounts.*
import work.socialhub.kmastodon.api.request.favourites.FavouritesFavouritesRequest
import work.socialhub.kmastodon.api.request.medias.MediasPostMediaRequest
import work.socialhub.kmastodon.api.request.notifications.NotificationsNotificationsRequest
import work.socialhub.kmastodon.api.request.search.SearchSearchRequest
import work.socialhub.kmastodon.api.request.statuses.*
import work.socialhub.kmastodon.api.request.timelines.TimelinesHashTagTimelineRequest
import work.socialhub.kmastodon.api.request.timelines.TimelinesHomeTimelineRequest
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.mastodon.define.MastodonNotificationType
import work.socialhub.planetlink.mastodon.define.MastodonReactionType.Favorite
import work.socialhub.planetlink.mastodon.define.MastodonReactionType.Reblog
import work.socialhub.planetlink.mastodon.define.MastodonVisibility
import work.socialhub.planetlink.mastodon.model.MastodonPaging
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.paging.BorderPaging
import work.socialhub.planetlink.model.paging.OffsetPaging
import work.socialhub.planetlink.model.request.CommentForm

class MastodonAction(
    account: Account,
    val auth: MastodonAuth,
) : AccountActionImpl(account) {

    /** List of Emoji  */
    private var emojisCache: List<Emoji>? = null

    // ============================================================== //
    // Account
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun userMe(): User {
        return proceed {
            val account = auth.accessor.accounts()
                .verifyCredentials()

            service().rateLimit.addInfo(
                SocialActionType.GetUserMe,
                MastodonMapper.rateLimit(account)
            )

            MastodonMapper.user(
                account.data,
                service()
            ).also { this.me = it }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun user(
        id: Identify
    ): User {
        return proceed {
            val account = auth.accessor.accounts().account(
                AccountsAccountRequest()
                    .also { it.id = id.id<String>() })

            service().rateLimit.addInfo(
                SocialActionType.GetUser,
                MastodonMapper.rateLimit(account)
            )
            MastodonMapper.user(account.data, service())
        }
    }

    /**
     * {@inheritDoc}
     * Parse Mastodon user's url, like:
     * https://mastodon.social/@uakihir0
     * https://mastodon.social/web/accounts/1223371
     */
    override fun user(
        url: String
    ): User {
        return proceed {
            var regex = "https://(.+?)/@(.+)".toRegex()
            var matcher = regex.find(url)

            if (matcher != null) {
                val host = matcher.groupValues[1]
                val screenName = matcher.groupValues[2]

                val format: String = ("@$screenName@$host")
                val users = searchUsers(format, Paging(10))
                users.entities.first { it.accountIdentify == format }

            } else {
                regex = "https://(.+?)/web/accounts/(.+)".toRegex()
                matcher = regex.find(url)

                if (matcher != null) {
                    val id = matcher.groupValues[2]
                    user(Identify(service, ID(id)))

                } else {
                    throw SocialHubException("this url is not supported format.")
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun followUser(
        id: Identify
    ) {
        proceedUnit {
            val relationship = auth.accessor.accounts().follow(
                AccountsFollowRequest().also { it.id = id.id<String>() }
            )
            service().rateLimit.addInfo(
                SocialActionType.FollowUser,
                MastodonMapper.rateLimit(relationship)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unfollowUser(
        id: Identify
    ) {
        proceedUnit {
            val relationship = auth.accessor.accounts().unfollow(
                AccountsUnfollowRequest().also { it.id = id.id<String>() }
            )
            service().rateLimit.addInfo(
                SocialActionType.UnfollowUser,
                MastodonMapper.rateLimit(relationship)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun muteUser(
        id: Identify
    ) {
        proceedUnit {
            val relationship = auth.accessor.accounts().mute(
                AccountsMuteRequest().also { it.id = id.id<String>() }
            )
            service().rateLimit.addInfo(
                SocialActionType.MuteUser,
                MastodonMapper.rateLimit(relationship)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unmuteUser(
        id: Identify
    ) {
        proceedUnit {
            val relationship = auth.accessor.accounts().unmute(
                AccountsUnmuteRequest().also { it.id = id.id<String>() }
            )
            service().rateLimit.addInfo(
                SocialActionType.UnmuteUser,
                MastodonMapper.rateLimit(relationship)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun blockUser(
        id: Identify
    ) {
        proceedUnit {
            val relationship = auth.accessor.accounts().block(
                AccountsBlockRequest().also { it.id = id.id<String>() }
            )
            service().rateLimit.addInfo(
                SocialActionType.BlockUser,
                MastodonMapper.rateLimit(relationship)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unblockUser(
        id: Identify
    ) {
        proceedUnit {
            val relationship = auth.accessor.accounts().unblock(
                AccountsUnblockRequest().also { it.id = id.id<String>() }
            )
            service().rateLimit.addInfo(
                SocialActionType.UnblockUser,
                MastodonMapper.rateLimit(relationship),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun relationship(
        id: Identify
    ): Relationship {
        return proceed {
            val relationships = auth.accessor.accounts().relationships(
                AccountsRelationshipsRequest().also { it.addId(id.id<String>()) }
            )
            service().rateLimit.addInfo(
                SocialActionType.GetRelationship,
                MastodonMapper.rateLimit(relationships)
            )
            MastodonMapper.relationship(relationships.data[0])
        }
    }

    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun followingUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val range = range(paging)
            val accounts = auth.accessor.accounts().following(
                AccountsFollowingRequest().also {
                    it.id = id.id<String>()
                    it.range = range
                }
            )

            service().rateLimit.addInfo(
                UsersActionType.GetFollowingUsers,
                MastodonMapper.rateLimit(accounts)
            )

            MastodonMapper.users(
                accounts.data,
                service(),
                paging,
                accounts.link,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun followerUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val range = range(paging)
            val accounts = auth.accessor.accounts().followers(
                AccountsFollowersRequest().also {
                    it.id = id.id<String>()
                    it.range = range
                }
            )
            service().rateLimit.addInfo(
                UsersActionType.GetFollowerUsers,
                MastodonMapper.rateLimit(accounts),
            )
            MastodonMapper.users(
                accounts.data,
                service,
                paging,
                accounts.link,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun searchUsers(
        query: String,
        paging: Paging
    ): Pageable<User> {
        return proceed {
            val page = page(paging)
            val results = auth.accessor.search().search(
                SearchSearchRequest().also {
                    it.query = query
                    it.page = page
                }
            )
            service().rateLimit.addInfo(
                UsersActionType.SearchUsers,
                MastodonMapper.rateLimit(results)
            )
            MastodonMapper.users(
                results.data.accounts!!,
                service,
                paging,
                results.link
            )
        }
    }

    // ============================================================== //
    // Timeline
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun homeTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val range = range(paging)
            val status = auth.accessor.timelines().homeTimeline(
                TimelinesHomeTimelineRequest().also { it.range = range }
            )

            service().rateLimit.addInfo(
                TimeLineActionType.HomeTimeLine,
                MastodonMapper.rateLimit(status)
            )
            MastodonMapper.timeLine(
                status.data,
                service(),
                paging,
                status.link
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun mentionTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val range = range(paging)
            val notifications = auth.accessor.notifications().notifications(
                NotificationsNotificationsRequest().also {
                    it.range = range
                    // v3.5 から取得するものを指定可能
                    it.types = arrayOf(
                        MastodonNotificationType.MENTION.code
                    )
                    it.excludeTypes = arrayOf(
                        MastodonNotificationType.FOLLOW.code,
                        MastodonNotificationType.FOLLOW_REQUEST.code,
                        MastodonNotificationType.FAVOURITE.code,
                        MastodonNotificationType.REBLOG.code
                    )
                })

            val statuses = notifications.data
                .mapNotNull { it.status }
                .toTypedArray()

            service().rateLimit.addInfo(
                TimeLineActionType.MentionTimeLine,
                MastodonMapper.rateLimit(notifications)
            )

            MastodonMapper.timeLine(
                statuses,
                service(),
                paging,
                notifications.link
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userCommentTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val range = range(paging)
            val status = auth.accessor.accounts().statuses(
                AccountsStatusesRequest().also {
                    it.id = id.id<String>()
                    it.range = range
                }
            )

            service().rateLimit.addInfo(
                TimeLineActionType.UserCommentTimeLine,
                MastodonMapper.rateLimit(status),
            )

            MastodonMapper.timeLine(
                status.data,
                service,
                paging,
                status.link
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userLikeTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            // 自分の分しか取得できないので id が自分でない場合は例外
            if (id.isSameIdentify(userMeWithCache())) {

                val range = range(paging)
                val status = auth.accessor.favourites().favourites(
                    FavouritesFavouritesRequest().also {
                        it.range = range
                    }
                )
                service().rateLimit.addInfo(
                    TimeLineActionType.UserLikeTimeLine,
                    MastodonMapper.rateLimit(status)
                )

                MastodonMapper.timeLine(
                    status.data,
                    service,
                    paging,
                    status.link
                )
            } else {
                throw NotSupportedException(
                    "Sorry, user favorites timeline is only support only verified account on auth.accessor."
                )
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userMediaTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val range = range(paging)
            val status = auth.accessor.accounts().statuses(
                AccountsStatusesRequest().also {
                    it.id = id.id<String>()
                    it.onlyMedia = true
                    it.range = range
                }
            )
            service().rateLimit.addInfo(
                TimeLineActionType.UserMediaTimeLine,
                MastodonMapper.rateLimit(status)
            )
            MastodonMapper.timeLine(
                status.data,
                service,
                paging,
                status.link
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
            if (query.startsWith("#")) {

                // ハッシュタグのクエリの場合
                val range = range(paging)
                val results = auth.accessor.timelines().hashtagTimeline(
                    TimelinesHashTagTimelineRequest().also {
                        it.hashtag = query.substring(1)
                        it.range = range
                    }
                )

                MastodonMapper.timeLine(
                    results.data,
                    service,
                    paging,
                    results.link
                )
            } else {

                // それ以外は通常の検索を実施
                val page = page(paging)
                val results = auth.accessor.search().search(
                    SearchSearchRequest().also {
                        it.query = query
                        it.page = page
                    }
                )

                MastodonMapper.timeLine(
                    results.data.statuses!!,
                    service,
                    paging,
                    results.link
                )
            }
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
            val post = StatusesPostStatusRequest()

            // コンテンツ
            post.content = req.text

            // コンテンツ注意文言
            req.warning?.let {
                post.spoilerText = it
            }

            // 返信の処理
            req.replyId?.let {
                post.inReplyToId = it.value<String>()
            }

            // 公開範囲
            req.visibility?.let {
                post.visibility = it
            }

            // ダイレクトメッセージの場合
            if (req.isMessage) {
                post.visibility = MastodonVisibility.Direct.code
            }

            // 画像の処理
            if (req.images.isNotEmpty()) {

                // Mastodon はアップロードされた順番で配置が決定
                // -> 並列にメディアをアップロードせずに逐次上げる
                post.mediaIds = req.images.map { image ->
                    val attachment = auth.accessor.medias().postMedia(
                        MediasPostMediaRequest().also {
                            it.bytes = image.data
                            it.name = image.name
                        }
                    )
                    attachment.data.id!!
                }.toTypedArray()
            }

            // センシティブな内容
            if (req.isSensitive) {
                post.sensitive = true
            }

            // 投票
            req.poll?.let { poll ->
                post.pollOptions = poll.options.toTypedArray()
                post.pollMultiple = poll.multiple
                post.pollExpiresIn = poll.expiresIn * 60
            }

            val status = auth.accessor.statuses().postStatus(post)
            service().rateLimit.addInfo(
                SocialActionType.PostComment,
                MastodonMapper.rateLimit(status)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun comment(
        id: Identify
    ): Comment {
        return proceed {
            val status = auth.accessor.statuses().status(
                StatusesStatusRequest().also {
                    it.id = id.id<String>()
                }
            )
            service().rateLimit.addInfo(
                SocialActionType.GetComment,
                MastodonMapper.rateLimit(status)
            )
            MastodonMapper.comment(
                status.data,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     * Parse Mastodon Toot's url, like:
     * https://auth.accessor.social/@uakihir0/104681506368424218
     * https://auth.accessor.social/web/statuses/104681506368424218
     */
    override fun comment(
        url: String
    ): Comment {
        return proceed {
            var regex = "https://(.+?)/@(.+?)/(.+)".toRegex()
            var matcher = regex.find(url)

            if (matcher != null) {
                val id = matcher.groupValues[3]
                val identify = Identify(service(), ID(id))
                comment(identify)

            } else {
                regex = "https://(.+?)/web/statuses/(.+)".toRegex()
                matcher = regex.find(url)

                if (matcher != null) {
                    val id = matcher.groupValues[2]
                    val identify = Identify(service(), ID(id))
                    comment(identify)

                } else {
                    throw SocialHubException("this url is not supported format.")
                }
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
            val status = auth.accessor.statuses().favourite(
                StatusesFavouriteRequest().also {
                    it.id = id.id<String>()
                }
            )
            service().rateLimit.addInfo(
                SocialActionType.LikeComment,
                MastodonMapper.rateLimit(status),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unlikeComment(
        id: Identify
    ) {
        proceedUnit {
            val status = auth.accessor.statuses().unfavourite(
                StatusesUnfavouriteRequest().also {
                    it.id = id.id<String>()
                }
            )
            service().rateLimit.addInfo(
                SocialActionType.UnlikeComment,
                MastodonMapper.rateLimit(status),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun shareComment(
        id: Identify
    ) {
        proceedUnit {
            val status = auth.accessor.statuses().reblog(
                StatusesReblogRequest().also {
                    it.id = id.id<String>()
                }
            )
            service().rateLimit.addInfo(
                SocialActionType.ShareComment,
                MastodonMapper.rateLimit(status),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unshareComment(
        id: Identify
    ) {
        proceed {
            val status = auth.accessor.statuses().unreblog(
                StatusesUnreblogRequest().also {
                    it.id = id.id<String>()
                }
            )
            service().rateLimit.addInfo(
                SocialActionType.UnShareComment,
                MastodonMapper.rateLimit(status),
            )
        }
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

            if (Favorite.codes.contains(type)) {
                likeComment(id)
                return
            }
            if (Reblog.codes.contains(type)) {
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

            if (Favorite.codes.contains(type)) {
                unlikeComment(id)
                return
            }
            if (Reblog.codes.contains(type)) {
                unshareComment(id)
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
            val voids = auth.accessor.statuses().deleteStatus(
                StatusesDeleteStatusRequest().also {
                    it.id = id.id<String>()
                }
            )
            service().rateLimit.addInfo(
                SocialActionType.DeleteComment,
                MastodonMapper.rateLimit(voids)
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    fun commentContext(
        id: Identify
    ): Context {
        return proceed {
            val idString = if (id is Comment) {
                id.displayComment.id<String>()
            } else id.id<String>()

            val response = auth.accessor.statuses().context(
                StatusesContextRequest().also {
                    it.id = idString
                }
            )

            service().rateLimit.addInfo(
                SocialActionType.GetContext,
                MastodonMapper.rateLimit(response)
            )

            Context().also { c ->
                c.descendants = response.data.descendants?.map {
                    MastodonMapper.comment(it, service())
                } ?: listOf()
                c.ancestors = response.data.ancestors?.map {
                    MastodonMapper.comment(it, service())
                } ?: listOf()
                c.sort()
            }
        }
    }

    val emojis: List<Any>
        /**
         * {@inheritDoc}
         */
        get() {
            if (emojisCache != null) {
                return emojisCache
            }

            return proceed({

                val emojis: Response<Array<mastodon4j.entity.Emoji>> =
                    auth.accessor.emoji().getCustomEmojis()

                val model: MutableList<Emoji> = java.util.ArrayList<Emoji>()
                model.addAll(MastodonMapper.emojis(emojis.data))
                model.addAll(super.getEmojis())
                emojisCache = model
                emojisCache
            })
        }

    // ============================================================== //
    // Channel (List) API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getChannels(id: Identify?, paging: Paging?): Pageable<Channel> {
        return proceed({


            if (id != null) {
                val me: User = getUserMeWithCache()
                if (!me.getId().equals(id.getId())) {
                    throw NotSupportedException(
                        "Sorry, authenticated user only."
                    )
                }
            }

            val lists: Response<Array<mastodon4j.entity.List>> = auth.accessor.list().getLists()
            service().rateLimit.addInfo(GetChannels, MastodonMapper.rateLimit(lists))
            MastodonMapper.channels(lists.data, service)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getChannelTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({


            val range = range(paging)

            val status: Response<Array<Status>> = auth.accessor.timelines()
                .getListTimeline(id.getId() as String, range)
            service().rateLimit.addInfo(ChannelTimeLine, MastodonMapper.rateLimit(status))
            MastodonMapper.timeLine(
                status.data,
                service,
                paging,
                status.link
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getChannelUsers(id: Identify, paging: Paging?): Pageable<User> {
        return proceed({


            val limit: Long? = if ((paging != null)) paging.getCount() else null

            val users: Response<Array<mastodon4j.entity.Account>> = mastodon
                .list().getListAccounts(id.getId() as String, limit)
            service().rateLimit.addInfo(ChannelTimeLine, MastodonMapper.rateLimit(users))
            MastodonMapper.users(
                users.data,
                service,
                paging,
                users.link
            )
        })
    }

    // ============================================================== //
    // Message API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getMessageThread(paging: Paging?): Pageable<java.lang.Thread> {
        return proceed({


            val range = range(paging)

            val response: Response<Array<Conversation>> =
                auth.accessor.timelines().getConversations(range)

            val threads: MutableList<java.lang.Thread> = java.util.ArrayList<java.lang.Thread>()
            for (conv: Conversation in response.data) {
                // 最後のコメントを取得

                val comment: Comment = MastodonMapper
                    .comment(conv.getLastStatus(), service)

                // 「名前: コメント内容」のフォーマットで説明文を作成
                val description: String = (comment.getUser().getName()
                        + ": " + comment.getText().getDisplayText())

                val thread: MastodonThread = MastodonThread(service)
                thread.setLastUpdate(comment.getCreateAt())
                thread.setDescription(description)
                thread.setUsers(java.util.ArrayList<E>())
                thread.setLastComment(comment)
                thread.setId(conv.getId())
                threads.add(thread)

                // アカウントリストを設定
                for (account: mastodon4j.entity.Account? in conv.getAccounts()) {
                    val user: User = MastodonMapper.user(account, service)
                    thread.getUsers().add(user)
                }
            }

            val mpg: MastodonPaging = MastodonPaging.fromPaging(paging)
            MastodonMapper.withLink(mpg, response.link)

            val results: Pageable<java.lang.Thread> = Pageable()
            results.setEntities(threads)
            results.setPaging(mpg)
            results
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getMessageTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({
            var commentId: String? = null


            // Identify を直接作成した場合
            if (id.getId() is String) {
                commentId = id.getId() as String
            }
            // MastodonThread から呼び出した場合
            if (id is MastodonThread) {
                val th: MastodonThread = id as MastodonThread
                commentId = (th.getLastComment().getId()) as String
            }

            // ID が発見できない場合
            if (commentId == null) {
                val message: String = "matched id is not found."
                throw java.lang.IllegalStateException(message)
            }

            val response: Response<mastodon4j.entity.Context> =
                auth.accessor.getContext(commentId)

            val comments: MutableList<Comment> = java.util.ArrayList<Comment>()
            comments.addAll(java.util.Arrays.stream(response.data.getDescendants()) //
                .map { e -> MastodonMapper.comment(e, service) } //
                .collect(java.util.stream.Collectors.toList()))
            comments.addAll(java.util.Arrays.stream(response.data.getAncestors()) //
                .map { e -> MastodonMapper.comment(e, service) } //
                .collect(java.util.stream.Collectors.toList()))

            // 最後のコメントも追加
            if (id is MastodonThread) {
                comments.add((id as MastodonThread).getLastComment())
            }

            comments.sort(java.util.Comparator.comparing<Any, Any>(Comment::getCreateAt).reversed())
            service().rateLimit.addInfo(GetContext, MastodonMapper.rateLimit(response))

            val pageable: Pageable<Comment> = Pageable()
            pageable.setEntities(comments)
            pageable.setPaging(Paging())
            pageable.getPaging().setCount(0L)
            pageable.getPaging().setHasNext(false)
            pageable.getPaging().setHasPast(false)
            pageable
        })
    }

    /**
     * {@inheritDoc}
     */
    fun postMessage(req: CommentForm) {
        postComment(req)
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun setHomeTimeLineStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({


            val model: MastodonStream = MastodonStream()
            val stream: UserStream = auth.accessor.streaming().userStream().register(
                net.socialhub.service.auth.accessor.action.MastodonAction.MastodonCommentListener(callback, service),
                net.socialhub.service.auth.accessor.action.MastodonAction.MastodonConnectionListener(callback, model)
            )

            model.setStream(stream)
            model
        })
    }

    /**
     * {@inheritDoc}
     */
    fun setNotificationStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({


            val model: MastodonStream = MastodonStream()
            val stream: UserStream = auth.accessor.streaming().userStream().register(
                net.socialhub.service.auth.accessor.action.MastodonAction.MastodonNotificationListener(
                    callback,
                    service
                ),
                net.socialhub.service.auth.accessor.action.MastodonAction.MastodonConnectionListener(callback, model)
            )

            model.setStream(stream)
            model
        })
    }

    // ============================================================== //
    // Poll
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun votePoll(id: Identify, choices: List<Int?>) {
        proceed({

            val array: LongArray =
                choices.stream().mapToLong(java.util.function.ToLongFunction<Int> { e: Int? -> e }).toArray()
            auth.accessor.votePoll(id.getId() as String, array)
        })
    }

    // ============================================================== //
    // Other
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getTrends(limit: Int): List<Trend> {
        return proceed({

            val response: Response<Array<mastodon4j.entity.Trend>> =
                auth.accessor.trend().getTrends(limit.toLong())

            val results: MutableList<Trend> = java.util.ArrayList<Trend>()
            for (trend: mastodon4j.entity.Trend in response.data) {
                val model: Trend = Trend()
                model.setName("#" + trend.getName())
                model.setQuery("#" + trend.getName())

                val uses: Int = java.util.Arrays
                    .stream(trend.getHistory())
                    .map(History::getUses)
                    .reduce { a: Long, b: Long -> java.lang.Long.sum(a, b) }
                    .orElse(0L)
                    .intValue()

                model.setVolume(uses)
                results.add(model)
            }
            results
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getNotification(paging: Paging?): Pageable<Notification> {
        return proceed({


            val range = range(paging)

            val notifications: Response<Array<mastodon4j.entity.Notification>> =
                auth.accessor.notifications().getNotifications(
                    range,  // v3.5 から取得するものを指定可能
                    java.util.Arrays.asList(
                        MastodonNotificationType.FOLLOW.getCode(),
                        MastodonNotificationType.REBLOG.getCode(),
                        MastodonNotificationType.FAVOURITE.getCode()
                    ),  // 互換性のために記述
                    java.util.Arrays.asList(
                        MastodonNotificationType.MENTION.getCode(),
                        MastodonNotificationType.POLL.getCode()
                    ),
                    null
                )
            MastodonMapper.notifications(
                notifications.data,
                service,
                paging,
                notifications.link
            )
        })
    }

    /**
     * Get Notification (Single)
     * 通知情報を取得
     */
    fun getNotification(identify: Identify): Notification {
        return proceed({


            val notification: Response<mastodon4j.entity.Notification> =
                auth.accessor.notifications()
                    .getNotification(identify.getId().toString())
            MastodonMapper.notification(
                notification.data, service
            )
        })
    }

    // ============================================================== //
    // Request
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun request(): RequestAction {
        return MastodonRequest(getAccount())
    }

    // ============================================================== //
    // Only Mastodon
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getLocalTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({


            val range = range(paging)

            val status: Response<Array<Status>> = auth.accessor.getPublicTimeline(true, false, range)
            service().rateLimit.addInfo(MicroBlogActionType.LocalTimeLine, MastodonMapper.rateLimit(status))
            MastodonMapper.timeLine(
                status.data,
                service,
                paging,
                status.link
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getFederationTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({


            val range = range(paging)

            val status: Response<Array<Status>> = auth.accessor.getPublicTimeline(false, false, range)
            service().rateLimit.addInfo(MicroBlogActionType.FederationTimeLine, MastodonMapper.rateLimit(status))
            MastodonMapper.timeLine(
                status.data,
                service,
                paging,
                status.link
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun setLocalLineStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({


            val model: MastodonStream = MastodonStream()
            val stream: PublicStream = auth.accessor.streaming()
                .publicStream(true)
                .register(
                    net.socialhub.service.auth.accessor.action.MastodonAction.MastodonCommentListener(
                        callback,
                        service
                    ),
                    net.socialhub.service.auth.accessor.action.MastodonAction.MastodonConnectionListener(
                        callback,
                        model
                    )
                )

            model.setStream(stream)
            model
        })
    }

    /**
     * {@inheritDoc}
     */
    fun setFederationLineStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({


            val model: MastodonStream = MastodonStream()
            val stream: PublicStream = auth.accessor.streaming()
                .publicStream(false)
                .register(
                    net.socialhub.service.auth.accessor.action.MastodonAction.MastodonCommentListener(
                        callback,
                        service
                    ),
                    net.socialhub.service.auth.accessor.action.MastodonAction.MastodonConnectionListener(
                        callback,
                        model
                    )
                )

            model.setStream(stream)
            model
        })
    }

    /**
     * Register ServiceWorker endpoint.
     * サービスワーカーのエンドポイントを設定
     */
    fun registerSubscription(
        endpoint: String?, publicKey: String?, authSecret: String?
    ) {
        proceed({

            // All notification
            val alert: Alert = Alert()
            alert.setFollow(true)
            alert.setFavourite(true)
            alert.setReblog(true)
            alert.setMention(true)
            alert.setPoll(true)
            auth.accessor.notifications().pushSubscription(
                endpoint, publicKey, authSecret, alert
            )
        })
    }

    /**
     * Get user pinned comments.
     * ユーザーのピンされたコメントを取得
     */
    fun getUserPinedComments(id: Identify): List<Comment> {
        return proceed({

            val range: Range = Range()
            range.setLimit(100)

            val status: Response<Array<Status>> = auth.accessor.accounts().getStatuses(
                id.getId() as String, true, false, false, false, range
            )
            java.util.stream.Stream.of(status.data)
                .map { s -> MastodonMapper.comment(s, service) }
                .collect(java.util.stream.Collectors.toList())
        })
    }

    val service: mastodon4j.domain.Service
        /**
         * Get Service Type.
         * サービスの種類を取得
         */
        get() {
            return proceed({
                auth.accessor.service()
            })
        }

    // ============================================================== //
    // Classes
    // ============================================================== //
    // コメントに対してのコールバック設定
    internal class MastodonCommentListener(
        listener: EventCallback,
        service: Service
    ) : UserStreamListener, PublicStreamListener {
        private val listener: EventCallback
        private val service: Service

        init {
            this.listener = listener
            this.service = service
        }

        fun onUpdate(status: Status?) {
            if (listener is UpdateCommentCallback) {
                val comment: Comment = MastodonMapper.comment(status, service)
                val event: CommentEvent = CommentEvent(comment)
                (listener as UpdateCommentCallback).onUpdate(event)
            }
        }

        fun onDelete(id: Long) {
            if (listener is DeleteCommentCallback) {
                val event: IdentifyEvent = IdentifyEvent(id)
                (listener as DeleteCommentCallback).onDelete(event)
            }
        }
    }

    // 通信に対してのコールバック設定
    internal class MastodonConnectionListener(
        listener: EventCallback,
        stream: MastodonStream
    ) : LifeCycleListener {
        private val listener: EventCallback
        private val stream: MastodonStream

        init {
            this.listener = listener
            this.stream = stream
        }

        fun onConnect() {
            stream.setConnecting(true)
            if (listener is ConnectCallback) {
                (listener as ConnectCallback).onConnect()
            }
        }

        fun onDisconnect() {
            stream.setConnecting(false)
            if (listener is DisconnectCallback) {
                (listener as DisconnectCallback).onDisconnect()
            }
        }
    }

    // 通知に対してのコールバック設定
    internal class MastodonNotificationListener(
        listener: EventCallback,
        service: Service
    ) : UserStreamListener, PublicStreamListener {
        private val listener: EventCallback
        private val service: Service

        init {
            this.listener = listener
            this.service = service
        }

        fun onNotification(notification: mastodon4j.entity.Notification) {
            val type: MastodonNotificationType =
                MastodonNotificationType.of(notification.getType())

            if ((type === MastodonNotificationType.MENTION) || (
                        type === MastodonNotificationType.FOLLOW) || (
                        type === MastodonNotificationType.REBLOG) || (
                        type === MastodonNotificationType.FAVOURITE)
            ) {
                // Mention の場合は先に処理

                if (type === MastodonNotificationType.MENTION) {
                    if (listener is MentionCommentCallback) {
                        val model: Comment = MastodonMapper.comment(notification.getStatus(), service)
                        (listener as MentionCommentCallback).onMention(CommentEvent(model))
                    }
                    return
                }

                val model: Notification = MastodonMapper.notification(notification, service)

                when (type) {
                    FOLLOW -> {
                        if (listener is FollowUserCallback) {
                            (listener as FollowUserCallback).onFollow(
                                UserEvent(model.getUsers().get(0))
                            )
                        }
                        return
                    }

                    REBLOG, FAVOURITE -> {
                        if (listener is NotificationCommentCallback) {
                            (listener as NotificationCommentCallback).onNotification(
                                NotificationEvent(model)
                            )
                        }
                        return
                    }

                    else -> throw java.lang.IllegalStateException()
                }
            }
        }
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

        if ((e is MastodonException) && (e.message != null)) {
            return SocialHubException(e.message, e)

            // TODO: エラーメッセージが設定されているエラーである場合
        }

        throw SocialHubException(e)
    }

    companion object {

        // ============================================================== //
        // Paging
        // ============================================================== //
        private fun range(paging: Paging?): Range? {
            if (paging == null) {
                return null
            }

            val range = Range()
            range.limit = paging.count

            // BorderPaging
            if (paging is BorderPaging) {

                if (paging.sinceId != null) {
                    if (paging.hintNewer) {
                        range.minId = paging.sinceId.toString()
                    } else {
                        range.sinceId = paging.sinceId.toString()
                    }
                }
                if (paging.maxId != null) {
                    range.maxId = paging.maxId.toString()
                }
            }

            // MastodonPaging
            if (paging is MastodonPaging) {

                if (paging.minId != null) {
                    range.minId = paging.minId
                }
                if (paging.maxId != null) {
                    range.maxId = paging.maxId
                }
            }

            return range
        }

        private fun page(paging: Paging?): Page? {
            if (paging == null) {
                return null
            }

            val pg = Page()
            pg.limit = paging.count

            if (paging is OffsetPaging) {
                if (paging.offset != null) {
                    pg.offset = paging.offset
                }
            }
            return pg
        }
    }
}