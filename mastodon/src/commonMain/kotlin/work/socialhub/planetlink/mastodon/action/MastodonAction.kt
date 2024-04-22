package work.socialhub.planetlink.mastodon.action


import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.kmastodon.MastodonException
import work.socialhub.kmastodon.api.request.Page
import work.socialhub.kmastodon.api.request.Range
import work.socialhub.kmastodon.api.request.accounts.*
import work.socialhub.kmastodon.api.request.favourites.FavouritesFavouritesRequest
import work.socialhub.kmastodon.api.request.lists.ListsListAccountsRequest
import work.socialhub.kmastodon.api.request.lists.ListsListsRequest
import work.socialhub.kmastodon.api.request.medias.MediasPostMediaRequest
import work.socialhub.kmastodon.api.request.notifications.NotificationsNotificationRequest
import work.socialhub.kmastodon.api.request.notifications.NotificationsNotificationsRequest
import work.socialhub.kmastodon.api.request.notifications.NotificationsPostSubscriptionRequest
import work.socialhub.kmastodon.api.request.polls.PollsVotePollRequest
import work.socialhub.kmastodon.api.request.search.SearchSearchRequest
import work.socialhub.kmastodon.api.request.statuses.*
import work.socialhub.kmastodon.api.request.timelines.*
import work.socialhub.kmastodon.api.request.trends.TrendsTrendsRequest
import work.socialhub.kmastodon.entity.Alert
import work.socialhub.kmastodon.entity.Status
import work.socialhub.kmastodon.stream.MastodonEx.stream
import work.socialhub.kmastodon.stream.define.PublicType
import work.socialhub.kmastodon.stream.listener.PublicStreamListener
import work.socialhub.kmastodon.stream.listener.UserStreamListener
import work.socialhub.kmastodon.stream.listener.primitive.LifeCycleListener
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.DeleteCommentCallback
import work.socialhub.planetlink.action.callback.comment.MentionCommentCallback
import work.socialhub.planetlink.action.callback.comment.NotificationCommentCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback
import work.socialhub.planetlink.action.callback.user.FollowUserCallback
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.mastodon.define.MastodonActionType
import work.socialhub.planetlink.mastodon.define.MastodonNotificationType
import work.socialhub.planetlink.mastodon.define.MastodonNotificationType.*
import work.socialhub.planetlink.mastodon.define.MastodonReactionType.Favorite
import work.socialhub.planetlink.mastodon.define.MastodonReactionType.Reblog
import work.socialhub.planetlink.mastodon.define.MastodonVisibility
import work.socialhub.planetlink.mastodon.model.MastodonPaging
import work.socialhub.planetlink.mastodon.model.MastodonStream
import work.socialhub.planetlink.mastodon.model.MastodonThread
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.event.IdentifyEvent
import work.socialhub.planetlink.model.event.NotificationEvent
import work.socialhub.planetlink.model.event.UserEvent
import work.socialhub.planetlink.model.paging.BorderPaging
import work.socialhub.planetlink.model.paging.OffsetPaging
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.kmastodon.entity.Notification as MNotification

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
                    user(Identify(service(), ID(id)))

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
                service(),
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
                service(),
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
                        MENTION.code
                    )
                    it.excludeTypes = arrayOf(
                        FOLLOW.code,
                        MastodonNotificationType.FOLLOW_REQUEST.code,
                        FAVOURITE.code,
                        REBLOG.code
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
                service(),
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
                    service(),
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
                service(),
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
                    service(),
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
                    service(),
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
            post.status = req.text

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

    /**
     * {@inheritDoc}
     */
    val emojis: List<Emoji>
        get() {
            if (this.emojisCache != null) {
                return checkNotNull(this.emojisCache)
            }

            return proceed {
                val emojis = auth.accessor.emojis().customEmojis()

                mutableListOf<Emoji>().also {
                    it.addAll(MastodonMapper.emojis(emojis.data))
                    it.addAll(super.emojis())
                }.also { this.emojisCache = it }
            }
        }

    // ============================================================== //
    // Channel (List) API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun channels(
        id: Identify,
        paging: Paging
    ): Pageable<Channel> {
        return proceed {
            if (!id.isSameIdentify(userMeWithCache())) {
                throw NotSupportedException(
                    "Sorry, authenticated user only."
                )
            }

            val lists = auth.accessor.lists().lists(
                ListsListsRequest().also {
                    it.id = id.id<String>()
                }
            )
            service().rateLimit.addInfo(
                SocialActionType.GetChannels,
                MastodonMapper.rateLimit(lists),
            )
            MastodonMapper.channels(
                lists.data,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun channelTimeLine(
        id: Identify,
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val range = range(paging)
            val status = auth.accessor.timelines().listTimeline(
                TimelinesListTimelineRequest().also {
                    it.listId = id.id<String>()
                    it.range = range
                }
            )
            service().rateLimit.addInfo(
                TimeLineActionType.ChannelTimeLine,
                MastodonMapper.rateLimit(status),
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
    override fun channelUsers(
        id: Identify,
        paging: Paging
    ): Pageable<User> {
        return proceed {
            val users = auth.accessor.lists().listAccounts(
                ListsListAccountsRequest().also {
                    it.id = id.id<String>()
                    it.limit = paging.count
                }
            )
            service().rateLimit.addInfo(
                TimeLineActionType.ChannelTimeLine,
                MastodonMapper.rateLimit(users)
            )
            MastodonMapper.users(
                users.data,
                service(),
                paging,
                users.link
            )
        }
    }

    // ============================================================== //
    // Message API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun messageThread(
        paging: Paging
    ): Pageable<Thread> {
        return proceed {
            val range = range(paging)
            val response = auth.accessor.timelines().conversations(
                TimelinesConversationsRequest().also {
                    it.range = range
                }
            )

            val threads = mutableListOf<Thread>()
            for (conv in response.data) {

                // 最後のコメントを取得
                val last = conv.lastStatus!!
                val comment = MastodonMapper.comment(last, service())

                // 「名前: コメント内容」のフォーマットで説明文を作成
                val name = comment.user!!.name
                val text = comment.text!!.displayText
                val description = "${name}: ${text}"

                val thread = MastodonThread(service()).also {
                    it.lastUpdate = comment.createAt
                    it.description = description
                    it.users = mutableListOf()
                    it.lastComment = comment
                    it.id = ID(conv.id!!)
                }.also { threads.add(it) }

                // アカウントリストを設定
                conv.accounts?.forEach {
                    val user = MastodonMapper.user(it, service())
                    thread.users = (thread.users!! + user)
                }
            }

            val mpg = MastodonPaging.fromPaging(paging)
            MastodonMapper.withLink(mpg, response.link)

            Pageable<Thread>().also {
                it.entities = threads
                it.paging = mpg
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun messageTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            var commentId = id.id<String>()

            // MastodonThread の場合
            if (id is MastodonThread) {
                commentId = id.lastComment!!.id<String>()
            }

            val response = auth.accessor.statuses().context(
                StatusesContextRequest().also {
                    it.id = commentId
                }
            )

            val comments = mutableListOf<Comment>()
            comments.addAll(response.data.descendants!!
                .map { MastodonMapper.comment(it, service()) })
            comments.addAll(response.data.ancestors!!
                .map { MastodonMapper.comment(it, service()) })

            // 最後のコメントも追加
            if (id is MastodonThread) {
                comments.add(id.lastComment!!)
            }

            comments.sortBy { it.createAt }
            comments.reversed()

            service().rateLimit.addInfo(
                SocialActionType.GetContext,
                MastodonMapper.rateLimit(response)
            )

            Pageable<Comment>().also { pg ->
                pg.entities = comments.reversed()
                pg.paging = Paging(0).also {
                    it.isHasNew = false
                    it.isHasPast = false
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun postMessage(
        req: CommentForm
    ) {
        postComment(req)
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun homeTimeLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val stream = auth.accessor.stream().userStream()
            stream.register(
                MastodonCommentListener(callback, service()),
                MastodonConnectionListener(callback),
            )
            MastodonStream(stream)
        }
    }

    /**
     * {@inheritDoc}
     */
    fun notificationStream(
        callback: EventCallback
    ): Stream {
        TODO("")
    }

    // ============================================================== //
    // Poll
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun votePoll(
        id: Identify,
        choices: List<Int>
    ) {
        proceedUnit {
            val array = choices.toTypedArray()
            auth.accessor.polls().votePoll(
                PollsVotePollRequest().also {
                    it.id = id.id<String>()
                    it.choices = array
                }
            )
        }
    }

    // ============================================================== //
    // Other
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun trends(
        limit: Int
    ): List<Trend> {
        return proceed {
            val response = auth.accessor.trends().trends(
                TrendsTrendsRequest().also {
                    it.limit = limit
                }
            )

            mutableListOf<Trend>().also { r ->
                for (trend in response.data) {
                    r.add(Trend().also { tr ->
                        tr.name = "#${trend.name}"
                        tr.query = "#${trend.name}"
                        tr.volume = trend.history!!
                            .sumOf { it.uses ?: 0 }
                    })
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    fun notification(
        paging: Paging
    ): Pageable<Notification> {
        return proceed {
            val range = range(paging)
            val notifications = auth.accessor.notifications().notifications(
                NotificationsNotificationsRequest().also {
                    it.range = range
                    // v3.5 から取得するものを指定可能
                    it.types = arrayOf(
                        FOLLOW.code,
                        REBLOG.code,
                        FAVOURITE.code
                    )
                    // 互換性のために記述
                    it.excludeTypes = arrayOf(
                        MENTION.code,
                        MastodonNotificationType.POLL.code
                    )
                }
            )
            MastodonMapper.notifications(
                notifications.data,
                service(),
                paging,
                notifications.link
            )
        }
    }

    /**
     * Get Notification (Single)
     * 通知情報を取得
     */
    fun notification(
        identify: Identify
    ): Notification {
        return proceed {
            val notification = auth.accessor.notifications().notification(
                NotificationsNotificationRequest().also {
                    it.id = identify.id<String>()
                })
            MastodonMapper.notification(
                notification.data,
                service(),
            )
        }
    }

    // ============================================================== //
    // Request
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun request(): RequestAction {
        return MastodonRequest(account)
    }

    // ============================================================== //
    // Only Mastodon
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun localTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val range = range(paging)
            val status = auth.accessor.timelines().publicTimeline(
                TimelinesPublicTimelineRequest().also {
                    it.local = true
                    it.range = range
                }
            )
            service().rateLimit.addInfo(
                MastodonActionType.LocalTimeLine,
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
    fun federationTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val range = range(paging)
            val status = auth.accessor.timelines().publicTimeline(
                TimelinesPublicTimelineRequest().also {
                    it.local = false
                    it.range = range
                }
            )
            service().rateLimit.addInfo(
                MastodonActionType.FederationTimeLine,
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
    fun localLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val stream = auth.accessor.stream().publicStream(PublicType.LOCAL)
            stream.register(
                MastodonCommentListener(callback, service()),
                MastodonConnectionListener(callback),
            )
            MastodonStream(stream)
        }
    }

    /**
     * {@inheritDoc}
     */
    fun federationLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val stream = auth.accessor.stream().publicStream(PublicType.ALL)
            stream.register(
                MastodonCommentListener(callback, service()),
                MastodonConnectionListener(callback),
            )
            MastodonStream(stream)
        }
    }

    /**
     * Register ServiceWorker endpoint.
     * サービスワーカーのエンドポイントを設定
     */
    fun registerSubscription(
        endpoint: String?,
        publicKey: String?,
        authSecret: String?
    ) {
        proceedUnit {

            // All notification
            val alert = Alert().also {
                it.follow = true
                it.favourite = true
                it.reblog = true
                it.mention = true
                it.poll = true
            }

            auth.accessor.notifications().pushSubscription(
                NotificationsPostSubscriptionRequest().also {
                    it.endpoint = endpoint
                    it.p256dh = publicKey
                    it.auth = authSecret
                    it.alert = alert
                }
            )
        }
    }

    /**
     * Get user pinned comments.
     * ユーザーのピンされたコメントを取得
     */
    fun userPinedComments(
        id: Identify
    ): List<Comment> {
        return proceed {
            val range = Range()
            range.limit = 100

            val status = auth.accessor.accounts().statuses(
                AccountsStatusesRequest().also {
                    it.id = id.id<String>()
                    it.onlyPinned = true
                    it.range = range
                }
            )
            status.data.map {
                MastodonMapper.comment(it, service())
            }
        }
    }

    /**
     * Get Service Type.
     * サービスの種類を取得
     */
    fun serviceType(): String {
        return auth.accessor.service().name
    }

    // ============================================================== //
    // Classes
    // ============================================================== //
    // コメントに対してのコールバック設定
    internal class MastodonCommentListener(
        private val listener: EventCallback,
        private val service: Service
    ) : UserStreamListener,
        PublicStreamListener {

        override fun onUpdate(
            status: Status
        ) {
            if (listener is UpdateCommentCallback) {
                val comment = MastodonMapper.comment(status, service)
                listener.onUpdate(CommentEvent(comment))
            }
        }

        override fun onDelete(
            id: String
        ) {
            if (listener is DeleteCommentCallback) {
                listener.onDelete(IdentifyEvent(id))
            }
        }

        override fun onNotification(notification: MNotification) {}
    }

    // 通信に対してのコールバック設定
    internal class MastodonConnectionListener(
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
                listener.onError(SocialHubException(e))
            }
        }

        // 通知に対してのコールバック設定
        internal class MastodonNotificationListener(
            val listener: EventCallback,
            val service: Service
        ) : UserStreamListener,
            PublicStreamListener {

            override fun onNotification(notification: MNotification) {
                val type = MastodonNotificationType.of(notification.type!!)

                if ((type === MENTION) || (
                            type === FOLLOW) || (
                            type === REBLOG) || (
                            type === FAVOURITE)
                ) {
                    // Mention の場合は先に処理
                    if (type === MENTION) {
                        if (listener is MentionCommentCallback) {
                            val model = MastodonMapper.comment(notification.status!!, service)
                            listener.onMention(CommentEvent(model))
                        }
                        return
                    }

                    val model = MastodonMapper.notification(notification, service)

                    when (type) {
                        FOLLOW -> {
                            if (listener is FollowUserCallback) {
                                listener.onFollow(UserEvent(model.users!![0]))
                            }
                            return
                        }

                        REBLOG, FAVOURITE -> {
                            if (listener is NotificationCommentCallback) {
                                listener.onNotification(NotificationEvent(model))
                            }
                            return
                        }

                        else -> throw IllegalStateException()
                    }
                }
            }

            override fun onDelete(id: String) {}
            override fun onUpdate(status: Status) {}
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
        return SocialHubException(e)
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