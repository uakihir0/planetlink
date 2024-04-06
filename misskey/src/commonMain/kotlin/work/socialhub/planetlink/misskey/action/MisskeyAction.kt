package work.socialhub.planetlink.misskey.action


import kotlinx.coroutines.runBlocking
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.kmisskey.Misskey
import work.socialhub.kmisskey.MisskeyException
import work.socialhub.kmisskey.api.model.PollRequest
import work.socialhub.kmisskey.api.request.blocks.BlocksCreateRequest
import work.socialhub.kmisskey.api.request.blocks.BlocksDeleteRequest
import work.socialhub.kmisskey.api.request.favorites.FavoritesCreateRequest
import work.socialhub.kmisskey.api.request.favorites.FavoritesDeleteRequest
import work.socialhub.kmisskey.api.request.files.FilesCreateRequest
import work.socialhub.kmisskey.api.request.following.FollowingCreateRequest
import work.socialhub.kmisskey.api.request.following.FollowingDeleteRequest
import work.socialhub.kmisskey.api.request.hashtags.HashtagsTrendRequest
import work.socialhub.kmisskey.api.request.i.IFavoritesRequest
import work.socialhub.kmisskey.api.request.i.INotificationsRequest
import work.socialhub.kmisskey.api.request.i.IRequest
import work.socialhub.kmisskey.api.request.lists.UsersListsListRequest
import work.socialhub.kmisskey.api.request.lists.UsersListsShowRequest
import work.socialhub.kmisskey.api.request.meta.EmojisRequest
import work.socialhub.kmisskey.api.request.mutes.MutesCreateRequest
import work.socialhub.kmisskey.api.request.mutes.MutesDeleteRequest
import work.socialhub.kmisskey.api.request.notes.*
import work.socialhub.kmisskey.api.request.other.ServiceWorkerRegisterRequest
import work.socialhub.kmisskey.api.request.polls.PollsVoteRequest
import work.socialhub.kmisskey.api.request.protocol.PagingRequest
import work.socialhub.kmisskey.api.request.protocol.PagingTokenRequest
import work.socialhub.kmisskey.api.request.reactions.ReactionsCreateRequest
import work.socialhub.kmisskey.api.request.reactions.ReactionsDeleteRequest
import work.socialhub.kmisskey.api.request.users.*
import work.socialhub.kmisskey.api.response.users.UsersShowResponse
import work.socialhub.kmisskey.entity.Note
import work.socialhub.kmisskey.entity.contant.NotificationType
import work.socialhub.kmisskey.stream.MisskeyStream
import work.socialhub.kmisskey.stream.callback.*
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.MentionCommentCallback
import work.socialhub.planetlink.action.callback.comment.NotificationCommentCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.user.FollowUserCallback
import work.socialhub.planetlink.misskey.define.MisskeyReactionType.Favorite
import work.socialhub.planetlink.misskey.define.MisskeyReactionType.Renote
import work.socialhub.planetlink.misskey.model.MisskeyPaging
import work.socialhub.planetlink.misskey.model.MisskeyPoll
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.event.NotificationEvent
import work.socialhub.planetlink.model.event.UserEvent
import work.socialhub.planetlink.model.paging.OffsetPaging
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.kmisskey.entity.Notification as MNotification
import work.socialhub.kmisskey.entity.user.User as MUser

class MisskeyAction(
    account: Account,
    val auth: MisskeyAuth,
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
            val misskey = auth.accessor
            val response = misskey.accounts().i(IRequest())

            MisskeyMapper.user(
                response.data,
                misskey.host,
                service(),
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
            val misskey = auth.accessor
            val user: UsersShowResponse
            val idv = id.id?.value

            // User のアカウント名で取得する場合
            if ((idv is String) && idv.startsWith("@")) {
                val elem = idv.split("@")
                val host = if ((elem.size > 2)) elem[2] else misskey.host

                user = misskey.users().show(
                    UsersShowSingleRequest().also {
                        it.username = elem[1]
                        it.host = host
                    }).data
            } else {
                user = misskey.users().show(
                    UsersShowSingleRequest().also {
                        it.userId = id.id<String>()
                    }).data
            }
            MisskeyMapper.user(
                user,
                misskey.host,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     * https://misskey.io/@syuilo
     * https://misskey.io/@syuilo@misskey.io
     */
    override fun user(
        url: String
    ): User {
        return proceed {
            val regex = ("https://(.+?)/@(.+)").toRegex()
            val matcher = regex.find(url)

            if (matcher != null) {
                val host = matcher.groupValues[1]
                val identify = matcher.groupValues[2]

                if (identify.contains("@")) {
                    val format = ("@$identify")
                    return@proceed user(Identify(service())
                        .also { it.id = ID(format) })
                } else {
                    val format = ("@$identify@$host")
                    return@proceed user(Identify(service())
                        .also { it.id = ID(format) })
                }
            }
            throw SocialHubException("this url is not supported format.")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun followUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.following().create(
                FollowingCreateRequest()
                    .also { it.userId = id.id<String>() }
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
            auth.accessor.following().delete(
                FollowingDeleteRequest()
                    .also { it.userId = id.id<String>() }
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
            auth.accessor.mutes().create(
                MutesCreateRequest()
                    .also { it.userId = id.id<String>() }
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
            auth.accessor.mutes().delete(
                MutesDeleteRequest()
                    .also { it.userId = id.id<String>() }
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
            auth.accessor.blocks().create(
                BlocksCreateRequest()
                    .also { it.userId = id.id<String>() }
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
            auth.accessor.blocks().delete(
                BlocksDeleteRequest()
                    .also { it.userId = id.id<String>() }
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
            val response = auth.accessor.users().relation(
                UsersRelationRequest().also {
                    it.userId = arrayOf(
                        id.id<String>()
                    )
                })
            MisskeyMapper.relationship(
                response.data[0]
            )
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
        paging: Paging
    ): Pageable<User> {
        return proceed {
            val misskey = auth.accessor
            val request = UsersFollowingsRequest()

            setPaging(request, paging)
            val response = misskey.users().followings(
                request.also { it.userId = id.id<String>() })

            MisskeyMapper.users(
                response.data.mapNotNull { it.followee },
                misskey.host,
                service(),
                paging,
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
            val misskey = auth.accessor
            val request = UsersFollowersRequest()

            setPaging(request, paging)
            val response = misskey.users().followers(
                request.also { it.userId = id.id<String>() })

            MisskeyMapper.users(
                response.data.mapNotNull { it.follower },
                misskey.host,
                service(),
                paging,
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
            val misskey = auth.accessor
            val request = UsersSearchRequest()

            paging.count?.let {
                request.limit = it
                if (it > 100) {
                    request.limit = 100
                }
            }

            if (paging is OffsetPaging) {
                paging.offset?.let {
                    request.offset = it
                }
            }

            request.query = query
            val response = misskey.users().search(request)

            val results = MisskeyMapper.users(
                response.data.toList(),
                misskey.host,
                service(),
                paging,
            )

            results.paging = OffsetPaging.fromPaging(paging)
            results
        }
    }

    // ============================================================== //
    // Timeline
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun homeTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val misskey = auth.accessor
            val request = NotesTimelineRequest()

            setPaging(request, paging)
            val response = misskey.notes().timeline(request)

            MisskeyMapper.timeLine(
                response.data
                    // Remove PR featured notes.
                    .filter { it.featuredId == null }
                    .filter { it.prId == null }
                    .toList(),
                misskey.host,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun mentionTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val misskey = auth.accessor
            val request = INotificationsRequest()

            setPaging(request, paging)
            request.markAsRead = true
            request.includeTypes = arrayOf(
                NotificationType.REPLY.code
            )

            val response = misskey.accounts().iNotifications(request)
            MisskeyMapper.mentions(
                response.data.toList(),
                misskey.host,
                service(),
                paging,
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
            val misskey = auth.accessor
            val request = UsersNotesRequest()
                .also { it.userId = id.id<String>() }

            setPaging(request, paging)
            val response = misskey.notes().users(request)

            MisskeyMapper.timeLine(
                response.data.toList(),
                misskey.host,
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
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            // FIXME: 自分のいいねのみを取得
            val misskey = auth.accessor
            val request = IFavoritesRequest()

            setPaging(request, paging)
            val response = misskey.accounts().iFavorites(request)

            MisskeyMapper.timeLine(
                response.data.map { it.note },
                misskey.host,
                service(),
                paging,
            )
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
            val misskey = auth.accessor
            val request = UsersNotesRequest().also {
                it.userId = id.id<String>()
                it.withFiles = true
            }

            setPaging(request, paging)
            val response = misskey.notes().users(request)
            MisskeyMapper.timeLine(
                response.data.toList(),
                misskey.host,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun searchTimeLine(
        query: String,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val misskey = auth.accessor
            val request = NotesSearchRequest()
                .also { it.query = query }
            setPaging(request, paging)

            val response = misskey.notes().search(request)
            MisskeyMapper.timeLine(
                response.data.toList(),
                misskey.host,
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
        if (req.isMessage) {
            postMessage(req)
            return
        }

        proceed {
            val misskey = auth.accessor
            val builder = NotesCreateRequest()
                .also {
                    it.text = req.text
                    it.cw = req.warning
                }

            // 返信の処理
            req.replyId?.let {
                val id = it.value<String>()
                builder.replyId = id
            }

            // 引用 RT の場合はその ID を設定
            req.quoteId?.let {
                val id = it.value<String>()
                builder.renoteId = id
            }

            // 画像の処理
            if (req.images.isNotEmpty()) {

                // TODO: 並列処理
                // 画像を順次アップロードする
                val medias = req.images.map { image ->
                    misskey.files().create(
                        FilesCreateRequest().also {
                            it.sensitive = req.isSensitive
                            it.bytes = image.data
                            it.name = image.name
                            it.force = true
                        }).data.id
                }

                builder.fileIds = medias.toTypedArray()
            }

            // 投票
            req.poll?.let { poll ->
                builder.poll = PollRequest().also {
                    it.choices = poll.options.toTypedArray()
                    it.multiple = poll.multiple
                    it.expiredAfter = poll.expiresIn * 1000 * 60
                }
            }

            // 公開範囲
            req.visibility?.let {
                builder.visibility = it
            }

            misskey.notes().create(builder)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun comment(
        id: Identify
    ): Comment {
        return proceed {
            val misskey = auth.accessor
            val response = misskey.notes().show(
                NotesShowRequest().also {
                    it.noteId = id.id<String>()
                })

            MisskeyMapper.comment(
                response.data,
                misskey.host,
                service()
            )
        }
    }

    /**
     * {@inheritDoc}
     * Parse Misskey Post's url, like:
     * https://misskey.io/notes/8axwbcxiff
     */
    override fun comment(
        url: String
    ): Comment {
        return proceed {
            val service = account.service
            val regex = ("https://(.+?)/notes/(.+)").toRegex()
            val matcher = regex.find(url)

            if (matcher != null) {
                return@proceed comment(
                    Identify(service).also {
                        it.id = ID(matcher.groupValues[2])
                    })
            }

            throw SocialHubException("this url is not supported format.")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun likeComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.favorites().create(
                FavoritesCreateRequest().also {
                    it.noteId = id.id<String>()
                })
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unlikeComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.favorites().delete(
                FavoritesDeleteRequest().also {
                    it.noteId = id.id<String>()
                })
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun shareComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.notes().create(
                NotesCreateRequest().also {
                    it.renoteId = id.id<String>()
                })
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unshareComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.notes().unrenote(
                NoteUnrenoteRequest().also {
                    it.noteId = id.id<String>()
                })
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun reactionComment(
        id: Identify,
        reaction: String,
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (Favorite.codes.contains(type)) {
                likeComment(id)
                return
            }
            if (Renote.codes.contains(type)) {
                shareComment(id)
                return
            }

            auth.accessor.reactions().create(
                ReactionsCreateRequest().also {
                    it.noteId = id.id<String>()
                    it.reaction = reaction
                })
            return
        }

        throw SocialHubException("not supported.")
    }

    /**
     * {@inheritDoc}
     */
    override fun unreactionComment(
        id: Identify,
        reaction: String,
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (Favorite.codes.contains(type)) {
                unlikeComment(id)
                return
            }
            if (Renote.codes.contains(type)) {
                unshareComment(id)
                return
            }

            // ユーザーごとにリアクションは一つのみ
            auth.accessor.reactions().delete(
                ReactionsDeleteRequest().also {
                    it.noteId = id.id<String>()
                })
            return
        }

        throw SocialHubException("not supported.")
    }

    /**
     * {@inheritDoc}
     */
    override fun deleteComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.notes().delete(
                NotesDeleteRequest().also {
                    it.noteId = id.id<String>()
                })
        }
    }

    /**
     * Get List of Emojis
     */
    val emojis: List<Emoji>
        get() {
            if (this.emojisCache != null) {
                return checkNotNull(this.emojisCache)
            }

            return proceed {
                val misskey = auth.accessor

                // V13 からは emojis エンドポイントから取得
                val response = misskey.meta()
                    .emojis(EmojisRequest())

                this.emojisCache = MisskeyMapper.emojis(
                    response.data.emojis.toList()
                )

                checkNotNull(this.emojisCache)
            }
        }

    /**
     * {@inheritDoc}
     */
    fun commentContext(
        id: Identify
    ): Context {
        return proceed {
            val misskey = auth.accessor

            val displayId = (if ((id is Comment))
                (id.displayComment.id<String>())
            else id.id<String>())

            val conversation = misskey.notes().conversation(
                NotesConversationRequest().also {
                    it.noteId = displayId
                    it.limit = 100
                })

            val children = misskey.notes().children(
                NotesChildrenRequest().also {
                    it.noteId = displayId
                    it.limit = 100
                })


            // Children の後に続くコメント類を取得
            val descendants = children.data.toMutableList<Note>()

            moreReplies(
                misskey,
                descendants,
                descendants
                    .filter { hasMoreReactionPossibility(it) }
                    .map { it.id }
            )

            // コンテキストの組み立て
            val context = Context()
            context.ancestors = conversation.data
                .map { MisskeyMapper.comment(it, misskey.host, service()) }
            context.descendants = descendants
                .map { MisskeyMapper.comment(it, misskey.host, service()) }

            // 並び替えを実行
            context.sort()
            context
        }
    }

    // さらなる返信を探して notes に追加
    private fun moreReplies(
        misskey: Misskey,
        notes: MutableList<Note>,
        nextIds: List<String>,
    ) {
        // No more replies to get (or limit)
        if ((notes.size >= 100) || (nextIds.isEmpty())) {
            return
        }

        val results = mutableListOf<Note>()

        // 各 NextID 毎に検索
        for (nextId in nextIds) {
            val children = misskey.notes().children(
                NotesChildrenRequest()
                    .also {
                        it.noteId = nextId
                        it.limit = 100
                    }
            )

            children.data.forEach { child ->
                if (notes.none { it.id == child.id }) {
                    results.add(child)
                }
            }
        }

        val ids = results
            .filter { hasMoreReactionPossibility(it) }
            .map { it.id }

        notes.addAll(results)
        moreReplies(misskey, notes, ids)
    }

    // このノートには更にリアクションが付く可能性があるかどうか？
    private fun hasMoreReactionPossibility(note: Note): Boolean {
        return (note.repliesCount > 0) || (note.renoteCount > 0)
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
            val misskey = auth.accessor
            val me = userMeWithCache()

            if (me.id<String>() != id.id<String>()) {
                throw NotSupportedException(
                    "Sorry, authenticated user only."
                )
            }

            // リスト一覧はページングには非対応
            val response = misskey.lists().list(UsersListsListRequest())
            MisskeyMapper.channels(
                response.data.toList(),
                service()
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun channelTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val misskey: Misskey = auth.accessor
            val request = NotesUserListTimelineRequest()
                .also { it.listId = id.id<String>() }

            setPaging(request, paging)
            val response = misskey.notes().userListTimeline(request)

            MisskeyMapper.timeLine(
                response.data.toList(),
                misskey.host,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun channelUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val misskey: Misskey = auth.accessor

            val list = misskey.lists().show(
                UsersListsShowRequest().also {
                    it.listId = id.id<String>()
                })

            val users = misskey.users().show(
                UsersShowMultipleRequest().also {
                    it.userIds = list.data.userIds!!
                }
            )
            MisskeyMapper.users(
                users.data.toList(),
                misskey.host,
                service(),
                paging,
            )
        }
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
        // MisskeyPoll 以外は例外
        if (id !is MisskeyPoll) {
            throw SocialHubException("Not support default identify object in Misskey.")
        }

        proceed {
            for (choice in choices) {
                auth.accessor.polls().pollsVote(
                    PollsVoteRequest().also {
                        it.noteId = id.noteId
                        it.choice = choice
                    }
                )
            }
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
            val misskey: Misskey = auth.accessor
            val response = misskey.hashtags().trend(
                HashtagsTrendRequest()
            )

            response.data.map {
                MisskeyMapper.trend(it)
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
            val misskey = auth.accessor
            // TODO: 並列でリクエストを実行
            val emojis = this.emojis

            val builder = INotificationsRequest()
            setPaging(builder, paging)

            builder.markAsRead = true
            builder.includeTypes = arrayOf(
                NotificationType.FOLLOW.code,
                NotificationType.REACTION.code,
                NotificationType.RENOTE.code
            )

            val response = misskey.accounts()
                .iNotifications(builder)

            MisskeyMapper.notifications(
                response.data.toList(),
                emojis,
                misskey.host,
                service(),
                paging,
            )
        }
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
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey)

            val commentsListener = MisskeyCommentsListener(
                callback,
                service(),
                misskey.host,
            )

            val connectionListener = MisskeyConnectionListener(callback) {
                runBlocking { stream.homeTimeLine(commentsListener) }
            }

            setStreamConnectionCallback(stream, connectionListener)
            work.socialhub.planetlink.misskey.model.MisskeyStream(stream)
        }
    }

    /**
     * {@inheritDoc}
     */
    fun notificationStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey)

            val notificationListener = MisskeyNotificationListener(
                callback,
                emojis,
                service(),
                misskey.host,
                userMeWithCache()
            )

            val connectionListener = MisskeyConnectionListener(callback) {
                runBlocking { stream.main(notificationListener) }
            }

            setStreamConnectionCallback(stream, connectionListener)
            work.socialhub.planetlink.misskey.model.MisskeyStream(stream)
        }
    }

    // ============================================================== //
    // Another TimeLines
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun localTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val misskey = auth.accessor
            val request = NotesLocalTimelineRequest()

            setPaging(request, paging)
            val response = misskey.notes().localTimeline(request)

            MisskeyMapper.timeLine(
                response.data.toList(),
                misskey.host,
                service(),
                paging,
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
            val misskey = auth.accessor
            val request = NotesGlobalTimelineRequest()

            setPaging(request, paging)
            val response = misskey.notes().globalTimeline(request)

            MisskeyMapper.timeLine(
                response.data.toList(),
                misskey.host,
                service(),
                paging,
            )
        }
    }

    /**
     * Get Featured Timeline
     */
    fun featuredTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val misskey = auth.accessor
            val request = NotesFeaturedRequest()

            paging.count?.let {
                request.limit = it
                if (it > 100) {
                    request.limit = 100
                }
            }

            val response = misskey.notes().featured(request)
            val results = MisskeyMapper.timeLine(
                response.data.toList(),
                misskey.host,
                service(),
                paging,
            )

            results.paging = OffsetPaging.fromPaging(paging)
                .also { it.isHasNew = false }
            results
        }
    }

    /**
     * {@inheritDoc}
     */
    fun localLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey)

            val commentsListener = MisskeyCommentsListener(
                callback,
                service(),
                misskey.host,
            )

            val connectionListener = MisskeyConnectionListener(callback) {
                runBlocking { stream.localTimeline(commentsListener) }
            }
            setStreamConnectionCallback(stream, connectionListener)
            work.socialhub.planetlink.misskey.model.MisskeyStream(stream)
        }
    }

    /**
     * {@inheritDoc}
     */
    fun federationLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey)

            val commentsListener = MisskeyCommentsListener(
                callback,
                service(),
                misskey.host,
            )
            val connectionListener = MisskeyConnectionListener(callback) {
                runBlocking { stream.globalTimeline(commentsListener) }
            }

            setStreamConnectionCallback(stream, connectionListener)
            work.socialhub.planetlink.misskey.model.MisskeyStream(stream)
        }
    }

    /**
     * Register ServiceWorker endpoint.
     * サービスワーカーのエンドポイントを設定
     */
    fun registerSubscription(
        endpoint: String,
        publicKey: String,
        authSecret: String
    ) {
        proceed {
            auth.accessor.other().serviceWorkerRegister(
                ServiceWorkerRegisterRequest().also {
                    it.endpoint = endpoint
                    it.publickey = publicKey
                    it.auth = authSecret
                }
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
        return MisskeyRequest(account)
    }

    // ============================================================== //
    // paging
    // ============================================================== //
    private fun setPaging(
        builder: PagingTokenRequest,
        paging: Paging
    ) {
        paging.count?.let {
            builder.limit = it
            if (it > 100) {
                builder.limit = 100
            }
        }

        if (paging is MisskeyPaging) {
            paging.untilId?.let {
                builder.untilId = it
            }
            paging.sinceId?.let {
                builder.sinceId = it
            }
        }
    }

    private fun setPaging(
        builder: PagingRequest,
        paging: Paging
    ) {
        paging.count?.let {
            builder.limit = it
            if (it > 100) {
                builder.limit = 100
            }
        }

        if (paging is MisskeyPaging) {
            paging.untilId?.let {
                builder.untilId = it
            }
            paging.sinceId?.let {
                builder.sinceId = it
            }
        }
    }

    private fun setStreamConnectionCallback(
        stream: MisskeyStream,
        callback: MisskeyConnectionListener,
    ) {
        stream.client.openedCallback = callback::onOpened
        stream.client.closedCallback = callback::onClosed
        stream.client.errorCallback = callback::onError
    }

    // ============================================================== //
    // Classes
    // ============================================================== //
    // コメントに対してのコールバック設定
    internal class MisskeyCommentsListener(
        val listener: EventCallback,
        val service: Service,
        val host: String
    ) : TimelineCallback {

        override fun onNoteUpdate(note: Note) {
            if (listener is UpdateCommentCallback) {
                val comment = MisskeyMapper.comment(note, host, service)
                listener.onUpdate(CommentEvent(comment))
            }
        }
    }

    // 通信に対してのコールバック設定
    internal class MisskeyConnectionListener(
        val listener: EventCallback,
        val runnable: () -> Unit
    ) : OpenedCallback,
        ClosedCallback,
        ErrorCallback {

        override fun onOpened() {
            if (listener is ConnectCallback) {
                listener.onConnect()
            }
            runnable()
        }

        override fun onClosed() {
            if (listener is DisconnectCallback) {
                listener.onDisconnect()
            }
        }

        override fun onError(e: Exception) {
            if (listener is ErrorCallback) {
                listener.onError(e)
            }
        }
    }

    // コメントに対してのコールバック設定
    internal class MisskeyNotificationListener(
        val listener: EventCallback,
        val emojis: List<Emoji>,
        val service: Service,
        val host: String,
        val me: User,
    ) : FollowedCallback,
        RenoteCallback,
        ReplayCallback,
        MentionCallback,
        NotificationCallback {

        override fun onFollowed(user: MUser) {
            if (listener is FollowUserCallback) {
                val model = MisskeyMapper.user(user, host, service)
                listener.onFollow(UserEvent(model))
            }
        }

        override fun onMention(note: Note) {
            // Mention と Reply が重複することを防止
            // -> 自分に対する Reply の場合は Reply に移譲
            if (note.reply?.user?.id == me.id<String>()) {
                return
            }

            if (listener is MentionCommentCallback) {
                val model = MisskeyMapper.comment(note, host, service)
                listener.onMention(CommentEvent(model))
            }
        }

        override fun onReply(note: Note) {
            if (listener is MentionCommentCallback) {
                val model = MisskeyMapper.comment(note, host, service)
                listener.onMention(CommentEvent(model))
            }
        }

        override fun onNotification(notification: MNotification) {
            // Reaction or Renote の場合のみ反応
            // (それ以外の場合は他でカバー済)

            if (notification.type == "reaction" ||
                notification.type == "renote"
            ) {
                if (listener is NotificationCommentCallback) {
                    val model = MisskeyMapper.notification(notification, emojis, host, service)
                    listener.onNotification(NotificationEvent(model))
                }
            }
        }

        override fun onRenote(note: Note) {
            // Renote は Notification にて反応
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
        if ((e is MisskeyException) && (e.message != null)) {
            return SocialHubException(e.message, e)

            // エラーメッセージが設定されているエラーである場合
            // if (me.getError() != null && me.getError().getError() != null) {
            //    val detail: ErrorDetail = me.getError().getError()
            //    se.setError(java.lang.Error(detail.getMessage()))
            // }
        }
        throw SocialHubException(e)
    }
}