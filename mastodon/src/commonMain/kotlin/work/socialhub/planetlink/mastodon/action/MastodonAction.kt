package work.socialhub.planetlink.mastodon.action


import work.socialhub.kmastodon.MastodonException
import work.socialhub.kmastodon.api.request.Page
import work.socialhub.kmastodon.api.request.Range
import work.socialhub.kmastodon.api.request.accounts.*
import work.socialhub.kmastodon.api.request.search.SearchSearchRequest
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.mastodon.model.MastodonPaging
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.paging.BorderPaging
import work.socialhub.planetlink.model.paging.OffsetPaging

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
    fun getHomeTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({


            val range = range(paging)

            val status: Response<Array<Status>> = auth.accessor.getHomeTimeline(range)
            service().rateLimit.addInfo(HomeTimeLine, MastodonMapper.rateLimit(status))
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
    fun getMentionTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({


            val range = range(paging)

            val status: Response<Array<mastodon4j.entity.Notification>> =
                auth.accessor.notifications().getNotifications(
                    range,  // v3.5 から取得するものを指定可能
                    listOf(
                        MastodonNotificationType.MENTION.getCode()
                    ),  // 互換性のために記述
                    java.util.Arrays.asList(
                        MastodonNotificationType.FOLLOW.getCode(),
                        MastodonNotificationType.FOLLOW_REQUEST.getCode(),
                        MastodonNotificationType.FAVOURITE.getCode(),
                        MastodonNotificationType.REBLOG.getCode()
                    ),
                    null
                )

            val statuses: List<Status> = java.util.stream.Stream.of(status.data)
                .map(mastodon4j.entity.Notification::getStatus)
                .collect(java.util.stream.Collectors.toList())

            service().rateLimit.addInfo(MentionTimeLine, MastodonMapper.rateLimit(status))
            MastodonMapper.timeLine(
                statuses,
                service,
                paging,
                status.link
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getUserCommentTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({


            val range = range(paging)

            val status: Response<Array<Status>> = auth.accessor.accounts().getStatuses(
                id.getId() as String, false, false, false, false, range
            )
            service().rateLimit.addInfo(UserCommentTimeLine, MastodonMapper.rateLimit(status))
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
    fun getUserLikeTimeLine(id: Identify?, paging: Paging?): Pageable<Comment> {
        return proceed({
            if (id != null) {
                // 自分の分しか取得できないので id が自分でない場合は例外

                if (id.getId().equals(getUserMeWithCache().getId())) {


                    val range = range(paging)

                    val status: Response<Array<Status>> = auth.accessor.favourites().getFavourites(range)
                    service().rateLimit.addInfo(UserLikeTimeLine, MastodonMapper.rateLimit(status))

                    return@proceed MastodonMapper.timeLine(
                        status.data,
                        service,
                        paging,
                        status.link
                    )
                }
            }
            throw NotSupportedException(
                "Sorry, user favorites timeline is only support only verified account on auth.accessor."
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getUserMediaTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({


            val range = range(paging)

            val status: Response<Array<Status>> = auth.accessor.accounts().getStatuses(
                id.getId() as String, false, true, false, false, range
            )
            service().rateLimit.addInfo(UserMediaTimeLine, MastodonMapper.rateLimit(status))
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
    fun getSearchTimeLine(query: String, paging: Paging?): Pageable<Comment> {
        return proceed({


            if (query.startsWith("#")) {
                // ハッシュタグのクエリの場合

                val range = range(paging)
                val results: Response<Array<Status>> = auth.accessor.getHashtagTimeline(
                    query.substring(1), false, false, range
                )

                return@proceed MastodonMapper.timeLine(
                    results.data,
                    service,
                    paging,
                    results.link
                )
            } else {
                // それ以外は通常の検索を実施

                val page = page(paging)
                val results: Response<Results> = auth.accessor.search().search(
                    query, false, false, page
                )

                return@proceed MastodonMapper.timeLine(
                    results.data.getStatuses(),
                    service,
                    paging,
                    results.link
                )
            }
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


            val update: StatusUpdate = StatusUpdate()
            update.setContent(req.getText())

            // コンテンツ注意文言
            if (req.getWarning() != null) {
                update.setSpoilerText(req.getWarning())
            }

            // 返信の処理
            if (req.getReplyId() != null) {
                update.setInReplyToId(req.getReplyId() as String)
            }

            // 公開範囲
            if (req.getVisibility() != null) {
                update.setVisibility(req.getVisibility())
            }

            // ダイレクトメッセージの場合
            if (req.isMessage()) {
                update.setVisibility(MastodonVisibility.Direct.getCode())
            }

            // 画像の処理
            if (req.getImages() != null && !req.getImages().isEmpty()) {
                update.setMediaIds(java.util.ArrayList<E>())

                // Mastodon はアップロードされた順番で配置が決定
                // -> 並列にメディアをアップロードせずに逐次上げる
                req.getImages().forEach { image ->
                    val input: java.io.InputStream = ByteArrayInputStream(image.getData())
                    val attachment: Response<Attachment> = auth.accessor.media() //
                        .postMedia(input, image.getName(), null)
                    update.getMediaIds().add(attachment.data.getId())
                }
            }

            // センシティブな内容
            if (req.isSensitive()) {
                update.setSensitive(true)
            }

            // 投票
            if (req.getPoll() != null) {
                val poll: PollForm = req.getPoll()
                update.setPollOptions(poll.getOptions())
                update.setPollMultiple(poll.getMultiple())
                update.setPollExpiresIn(poll.getExpiresIn() * 60)
            }

            val status: Response<Status> = auth.accessor.statuses().postStatus(update)
            service().rateLimit.addInfo(PostComment, MastodonMapper.rateLimit(status))
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getComment(id: Identify): Comment {
        return proceed({

            val status: Response<Status> = auth.accessor.statuses().getStatus(id.getId() as String)


            service().rateLimit.addInfo(GetComment, MastodonMapper.rateLimit(status))
            MastodonMapper.comment(status.data, service)
        })
    }

    /**
     * {@inheritDoc}
     * Parse Mastodon Toot's url, like:
     * https://auth.accessor.social/@uakihir0/104681506368424218
     * https://auth.accessor.social/web/statuses/104681506368424218
     */
    fun getComment(url: String?): Comment {
        return proceed({

            run {
                val regex: java.util.regex.Pattern = java.util.regex.Pattern.compile("https://(.+?)/@(.+?)/(.+)")
                val matcher: java.util.regex.Matcher = regex.matcher(url)
                if (matcher.matches()) {
                    val id: Long = matcher.group(3).toLong()
                    val identify: Identify = Identify(service, id)
                    return@proceed getComment(identify)
                }
            }
            run {
                val regex: java.util.regex.Pattern = java.util.regex.Pattern.compile("https://(.+?)/web/statuses/(.+)")
                val matcher: java.util.regex.Matcher = regex.matcher(url)
                if (matcher.matches()) {
                    val id: Long = matcher.group(2).toLong()
                    val identify: Identify = Identify(service, id)
                    return@proceed getComment(identify)
                }
            }
            throw SocialHubException("this url is not supported format.")
        })
    }

    /**
     * {@inheritDoc}
     */
    fun likeComment(id: Identify) {
        proceed({


            val status: Response<Status> = auth.accessor.statuses().favourite(id.getId() as String)
            service().rateLimit.addInfo(LikeComment, MastodonMapper.rateLimit(status))
        })
    }

    /**
     * {@inheritDoc}
     */
    fun unlikeComment(id: Identify) {
        proceed({


            val status: Response<Status> = auth.accessor.statuses().unfavourite(id.getId() as String)
            service().rateLimit.addInfo(UnlikeComment, MastodonMapper.rateLimit(status))
        })
    }

    /**
     * {@inheritDoc}
     */
    fun shareComment(id: Identify) {
        proceed({


            val status: Response<Status> = auth.accessor.statuses().reblog(id.getId() as String)
            service().rateLimit.addInfo(ShareComment, MastodonMapper.rateLimit(status))
        })
    }

    /**
     * {@inheritDoc}
     */
    fun unshareComment(id: Identify) {
        proceed({


            val status: Response<Status> = auth.accessor.statuses().unreblog(id.getId() as String)
            service().rateLimit.addInfo(UnShareComment, MastodonMapper.rateLimit(status))
        })
    }

    /**
     * {@inheritDoc}
     */
    fun reactionComment(id: Identify, reaction: String?) {
        if (reaction != null && !reaction.isEmpty()) {
            val type: String = reaction.lowercase(Locale.getDefault())

            if (MastodonReactionType.Favorite.getCode().contains(type)) {
                likeComment(id)
                return
            }
            if (MastodonReactionType.Reblog.getCode().contains(type)) {
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
            val type: String = reaction.lowercase(Locale.getDefault())

            if (MastodonReactionType.Favorite.getCode().contains(type)) {
                unlikeComment(id)
                return
            }
            if (MastodonReactionType.Reblog.getCode().contains(type)) {
                unretweetComment(id)
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


            val voids: Response<java.lang.Void> = auth.accessor.statuses().deleteStatus(id.getId() as String)
            service().rateLimit.addInfo(DeleteComment, MastodonMapper.rateLimit(voids))
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getCommentContext(id: Identify): Context {
        return proceed({


            val response: Response<mastodon4j.entity.Context> = auth.accessor.getContext(
                (if ((id is Comment)) //
                    (id as Comment).getDisplayComment().getId() else id.getId())
            )

            service().rateLimit.addInfo(GetContext, MastodonMapper.rateLimit(response))

            val context: Context = Context()
            context.setDescendants(java.util.Arrays.stream(response.data.getDescendants())
                .map { e -> MastodonMapper.comment(e, service) }
                .collect(java.util.stream.Collectors.toList()))
            context.setAncestors(java.util.Arrays.stream(response.data.getAncestors())
                .map { e -> MastodonMapper.comment(e, service) }
                .collect(java.util.stream.Collectors.toList()))

            MapperUtil.sortContext(context)
            context
        })
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