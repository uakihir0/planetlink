package work.socialhub.planetlink.misskey.action


import misskey4j.entity.contant.NotificationType
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
import work.socialhub.kmisskey.api.request.i.IFavoritesRequest
import work.socialhub.kmisskey.api.request.i.INotificationsRequest
import work.socialhub.kmisskey.api.request.i.IRequest
import work.socialhub.kmisskey.api.request.meta.EmojisRequest
import work.socialhub.kmisskey.api.request.mutes.MutesCreateRequest
import work.socialhub.kmisskey.api.request.mutes.MutesDeleteRequest
import work.socialhub.kmisskey.api.request.notes.*
import work.socialhub.kmisskey.api.request.reactions.ReactionsCreateRequest
import work.socialhub.kmisskey.api.request.reactions.ReactionsDeleteRequest
import work.socialhub.kmisskey.api.request.users.*
import work.socialhub.kmisskey.api.response.users.UsersShowResponse
import work.socialhub.kmisskey.entity.Note
import work.socialhub.kmisskey.entity.share.Response
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.misskey.define.MisskeyReactionType.Favorite
import work.socialhub.planetlink.misskey.define.MisskeyReactionType.Renote
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.paging.OffsetPaging
import work.socialhub.planetlink.model.request.CommentForm

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
                    .filter { it.prId == null },
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
        val futures: MutableList<java.util.concurrent.Future<Response<Array<NotesChildrenResponse>>>> =
            java.util.ArrayList<java.util.concurrent.Future<Response<Array<NotesChildrenResponse>>>>()

        // 各 NextID 毎に検索
        for (nextId in nextIds) {
            futures.add(pool.submit(java.lang.Runnable {
                misskey.notes().children(
                    NotesChildrenRequest()
                        .noteId(nextId)
                        .limit(100L)
                        .build()
                )
            }))
        }

        // 各リクエストの結果を統合
        for (future in futures) {
            results.addAll(
                java.util.stream.Stream.of<Any>(future.get())
                    .map<Any>(Response::get)
                    .flatMap<Any>(java.util.function.Function<Any, java.util.stream.Stream<*>> { t: T? ->
                        java.util.stream.Stream.of(
                            t
                        )
                    })
                    .filter(java.util.function.Predicate<Any> { e: Any ->
                        notes.stream().noneMatch(java.util.function.Predicate<Note> { n: Note ->
                            n.getId().equals(e.getId())
                        })
                    })
                    .collect<List<Any>, Any>(java.util.stream.Collectors.toList<Any>())
            )
        }

        val ids: List<String> = results.stream()
            .filter(java.util.function.Predicate<Note> { note: Note -> this.hasMoreReactionPossibility(note) })
            .map<Any>(Note::getId).collect<List<String>, Any>(java.util.stream.Collectors.toList<Any>())

        notes.addAll(results)
        getMoreReplies(misskey, pool, notes, ids)
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
    fun getChannels(id: Identify?, paging: Paging?): Pageable<Channel> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service

            if (id != null) {
                val me: User = getUserMeWithCache()
                if (!me.getId().equals(id.getId())) {
                    throw NotSupportedException(
                        "Sorry, authenticated user only."
                    )
                }
            }

            // リスト一覧はページングには非対応
            val response: Response<Array<UsersListsListResponse>> =
                misskey.lists().list(UsersListsListRequest().build())
            MisskeyMapper.channels(response.get(), service)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getChannelTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service

            val builder: NotesUserListTimelineRequest.NotesUserListTimelineRequestBuilder =
                NotesUserListTimelineRequest()

            setPaging(builder, paging)
            builder.listId(id.getId() as String)

            val response: Response<Array<NotesUserListTimelineResponse>> =
                misskey.notes().userListTimeline(builder.build())
            MisskeyMapper.timeLine(
                response.get(),
                misskey.getHost(),
                service,
                paging
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getChannelUsers(id: Identify, paging: Paging?): Pageable<User> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service

            val list: Response<UsersListsShowResponse> =
                misskey.lists().show(
                    UsersListsShowRequest()
                        .listId(id.getId() as String)
                        .build()
                )

            val users: Response<Array<UsersShowResponse>> =
                misskey.users().show(
                    UsersShowMultipleRequest()
                        .userIds(list.get().getUserIds())
                        .build()
                )
            MisskeyMapper.users(
                java.util.stream.Stream.of(users.get())
                    .collect(java.util.stream.Collectors.toList()),
                misskey.getHost(),
                service,
                null
            )
        })
    }

    // ============================================================== //
    // Message API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getMessageThread(paging: Paging): Pageable<java.lang.Thread> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service
            val pool: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newCachedThreadPool()

            val groupsFuture: java.util.concurrent.Future<Response<Array<MessagingHistoryResponse>>> =
                pool.submit(java.lang.Runnable {
                    misskey.messages().history(
                        MessagingHistoryRequest()
                            .limit(100L)
                            .group(true)
                            .build()
                    )
                })

            val messagesFuture: java.util.concurrent.Future<Response<Array<MessagingHistoryResponse>>> =
                pool.submit(java.lang.Runnable {
                    misskey.messages().history(
                        MessagingHistoryRequest()
                            .limit(100L)
                            .group(false)
                            .build()
                    )
                })

            val groups: Response<Array<MessagingHistoryResponse>> = groupsFuture.get()
            val messages: Response<Array<MessagingHistoryResponse>> = messagesFuture.get()


            // ユーザーの一覧を取得
            val userMap: MutableMap<String, User> = java.util.HashMap<String, User>()
            val userIds: List<String> = java.util.stream.Stream.of(groups.get())
                .flatMap { e -> e.getGroup().getUserIds().stream() }
                .distinct().collect(java.util.stream.Collectors.toList())

            CollectionUtil.partitionList(userIds, 100).forEach { ids ->
                val users: Response<Array<UsersShowResponse>> =
                    misskey.users().show(
                        UsersShowMultipleRequest()
                            .userIds(ids)
                            .build()
                    )
                for (user in users.get()) {
                    val model: User = MisskeyMapper.user(
                        user,
                        misskey.getHost(),
                        service
                    )
                    userMap[user.getId()] = model
                }
            }


            val threads: MutableList<java.lang.Thread> = java.util.ArrayList<java.lang.Thread>()
            val me: User = getUserMeWithCache()

            for (group in groups.get()) {
                threads.add(
                    MisskeyMapper.thread(
                        group,
                        misskey.getHost(), me, userMap, service
                    )
                )
            }
            for (message in messages.get()) {
                threads.add(
                    MisskeyMapper.thread(
                        message,
                        misskey.getHost(), me, userMap, service
                    )
                )
            }

            paging.setHasPast(false)
            paging.setHasNext(false)

            val results: Pageable<java.lang.Thread> = Pageable()
            results.setEntities(threads)
            results.setPaging(paging)
            results
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getMessageTimeLine(id: Identify, paging: Paging?): Pageable<Comment> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service
            val isGroup = ((id is MisskeyThread)
                    && (id as MisskeyThread).isGroup())

            val builder: MessagingMessagesRequest.MessagingMessagesRequestBuilder =
                MessagingMessagesRequest()
            setPaging(builder, paging)

            builder.markAsRead(true)
            builder.userId(if (isGroup) null else id.getId())
            builder.groupId(if (isGroup) id.getId() else null)

            val response: Response<Array<MessagingMessagesResponse>> =
                misskey.messages().messages(builder.build())
            MisskeyMapper.messages(
                response.get(),
                misskey.getHost(),
                service,
                emojis,
                paging
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun postMessage(req: CommentForm) {
        if (!req.isMessage()) {
            postComment(req)
            return
        }

        proceed({
            val misskey: Misskey = auth.accessor
            var isGroup: Boolean? = null
            var targetId: String? = null

            if (req.getReplyId() is String) {
                targetId = req.getReplyId()

                // パラメータからグループか取得
                isGroup = req.getParams()
                    .get(MisskeyFormKey.MESSAGE_TYPE)
                    .equals(MisskeyFormKey.MESSAGE_TYPE_GROUP)
            }

            if (req.getReplyId() is MisskeyThread) {
                targetId = (req.getReplyId() as java.lang.Thread).getId()
                isGroup = (req.getReplyId() as MisskeyThread).isGroup()
            }

            // 必須パラメータが発見できなかった場合
            if (isGroup == null || targetId == null) {
                throw SocialHubException("Cannot found [targetId] or [isGroup] params.")
            }

            val builder: MessagingMessagesCreateRequest.MessagingMessagesCreateRequestBuilder =
                MessagingMessagesCreateRequest()

            // 画像の処理
            if ((req.getImages() != null) && !req.getImages().isEmpty()) {
                // ファイルは一つまで添付可能

                if (req.getImages().size() > 1) {
                    throw SocialHubException("Only support one file to send message in Misskey.")
                }

                // ファイルのアップロードを実行
                val media: MediaForm = req.getImages().get(0)
                val input: java.io.InputStream = java.io.ByteArrayInputStream(media.getData())
                val response: Response<FilesCreateResponse> = misskey.files()
                    .create(
                        FilesCreateRequest()
                            .isSensitive(req.isSensitive())
                            .name(media.getName())
                            .stream(input)
                            .force(true)
                            .build()
                    )

                builder.fileId(response.get().getId())
            }

            builder.userId(if (isGroup) null else targetId)
            builder.groupId(if (isGroup) targetId else null)

            builder.text(req.getText())
            misskey.messages().messagesCreate(builder.build())
        })
    }

    // ============================================================== //
    // Poll
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun votePoll(id: Identify, choices: List<Int?>) {
        // MisskeyPoll 以外のオブジェクトは例外

        if (id !is MisskeyPoll) {
            throw SocialHubException("Not support default identify object in Misskey.")
        }

        proceed({
            val misskey: Misskey = auth.accessor
            for (choice in choices) {
                misskey.polls().pollsVote(
                    PollsVoteRequest()
                        .noteId((id as MisskeyPoll).getNoteId())
                        .choice(choice)
                        .build()
                )
            }
        })
    }

    // ============================================================== //
    // Other
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getTrends(limit: Int?): List<Trend> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val response: Response<Array<HashtagsTrendResponse>> = misskey
                .hashtags().trend(HashtagsTrendRequest().build())
            java.util.stream.Stream.of(response.get())
                .map(MisskeyMapper::trend)
                .collect(java.util.stream.Collectors.toList())
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getNotification(paging: Paging?): Pageable<net.socialhub.core.model.Notification> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service
            val pool: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newCachedThreadPool()

            val emojisFuture: java.util.concurrent.Future<List<Emoji>> =
                pool.submit<List<Emoji>>(java.util.concurrent.Callable<List<Emoji>> { this.emojis })

            val builder: INotificationsRequest.INotificationsRequestBuilder =
                INotificationsRequest()
            setPaging(builder, paging)

            builder.markAsRead(true)
            builder.includeTypes(
                java.util.Arrays.asList(
                    NotificationType.FOLLOW.code(),
                    NotificationType.REACTION.code(),
                    NotificationType.RENOTE.code()
                )
            )

            val responseFuture: java.util.concurrent.Future<Response<Array<INotificationsResponse>>> =
                pool.submit(java.lang.Runnable {
                    misskey.accounts()
                        .iNotifications(builder.build())
                })

            val emojis: List<Emoji> = emojisFuture.get()
            val response: Response<Array<INotificationsResponse>> = responseFuture.get()
            MisskeyMapper.notifications(
                response.get(),
                emojis,
                misskey.getHost(),
                service,
                paging
            )
        })
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun setHomeTimeLineStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service
            val stream: MisskeyStream = misskey.stream()

            val commentsListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyCommentsListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyCommentsListener(
                    callback,
                    service, misskey.getHost()
                )
            val connectionListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener(
                    callback,
                    java.lang.Runnable {
                        stream.homeTimeLine(
                            commentsListener
                        )
                    })

            stream.setOpenedCallback(connectionListener)
            stream.setClosedCallback(connectionListener)
            MisskeyStream(stream)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun setNotificationStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service
            val stream: MisskeyStream = misskey.stream()

            val notificationListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyNotificationListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyNotificationListener(
                    callback, emojis,
                    service, misskey.getHost(),
                    getUserMeWithCache()
                )

            val connectionListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener(
                    callback,
                    java.lang.Runnable {
                        stream.main(
                            notificationListener
                        )
                    })

            stream.setOpenedCallback(connectionListener)
            stream.setClosedCallback(connectionListener)
            stream.setErrorCallback(connectionListener)
            MisskeyStream(stream)
        })
    }

    // ============================================================== //
    // Another TimeLines
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun getLocalTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service

            val builder: NotesLocalTimelineRequest.NotesLocalTimelineRequestBuilder =
                NotesLocalTimelineRequest()

            setPaging(builder, paging)
            val response: Response<Array<NotesLocalTimelineResponse>> =
                misskey.notes().localTimeline(builder.build())
            MisskeyMapper.timeLine(
                response.get(),
                misskey.getHost(),
                service,
                paging
            )
        })
    }

    /**
     * {@inheritDoc}
     */
    fun getFederationTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service

            val builder: NotesGlobalTimelineRequest.NotesGlobalTimelineRequestBuilder =
                NotesGlobalTimelineRequest()

            setPaging(builder, paging)
            val response: Response<Array<NotesGlobalTimelineResponse>> =
                misskey.notes().globalTimeline(builder.build())
            MisskeyMapper.timeLine(
                response.get(),
                misskey.getHost(),
                service,
                paging
            )
        })
    }

    /**
     * Get Featured Timeline
     */
    fun getFeaturedTimeLine(paging: Paging?): Pageable<Comment> {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service

            val builder: NotesFeaturedRequest.NotesFeaturedRequestBuilder =
                NotesFeaturedRequest()

            if (paging != null) {
                if (paging.getCount() != null) {
                    builder.limit(paging.getCount())
                    if (paging.getCount() > 100) {
                        builder.limit(100L)
                    }
                }
                if (paging is OffsetPaging) {
                    val pg: OffsetPaging? = paging as OffsetPaging?
                    if (pg.getOffset() != null) {
                        builder.offset(pg.getOffset())
                    }
                }
            }

            val response: Response<Array<NotesFeaturedResponse>> =
                misskey.notes().featured(builder.build())

            val results: Pageable<Comment> = MisskeyMapper.timeLine(
                response.get(),
                misskey.getHost(),
                service,
                paging
            )

            results.setPaging(OffsetPaging.fromPaging(paging))
            results.getPaging().setHasNew(false)
            results
        })
    }

    /**
     * {@inheritDoc}
     */
    fun setLocalLineStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service
            val stream: MisskeyStream = misskey.stream()

            val commentsListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyCommentsListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyCommentsListener(
                    callback,
                    service, misskey.getHost()
                )
            val connectionListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener(
                    callback,
                    java.lang.Runnable {
                        stream.localTimeline(
                            commentsListener
                        )
                    })

            stream.setOpenedCallback(connectionListener)
            stream.setClosedCallback(connectionListener)
            stream.setErrorCallback(connectionListener)
            MisskeyStream(stream)
        })
    }

    /**
     * {@inheritDoc}
     */
    fun setFederationLineStream(callback: EventCallback?): net.socialhub.core.model.Stream {
        return proceed({
            val misskey: Misskey = auth.accessor
            val service: Service = account.service
            val stream: MisskeyStream = misskey.stream()

            val commentsListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyCommentsListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyCommentsListener(
                    callback,
                    service, misskey.getHost()
                )
            val connectionListener: net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener =
                net.socialhub.service.misskey.action.MisskeyAction.MisskeyConnectionListener(
                    callback,
                    java.lang.Runnable {
                        stream.globalTimeline(
                            commentsListener
                        )
                    })

            stream.setOpenedCallback(connectionListener)
            stream.setClosedCallback(connectionListener)
            stream.setErrorCallback(connectionListener)
            MisskeyStream(stream)
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
            val misskey: Misskey = auth.accessor
            misskey.other().serviceWorkerRegister(
                ServiceWorkerRegisterRequest()
                    .endpoint(endpoint)
                    .publickey(publicKey)
                    .auth(authSecret)
                    .build()
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
        return MisskeyRequest(account)
    }

    // ============================================================== //
    // paging
    // ============================================================== //
    private fun setPaging(builder: PagingBuilder<*>, paging: Paging?) {
        if (paging != null) {
            if (paging.getCount() != null) {
                builder.limit(paging.getCount())
                if (paging.getCount() > 100) {
                    builder.limit(100L)
                }
            }
            if (paging is MisskeyPaging) {
                val mp: MisskeyPaging = paging as MisskeyPaging
                if (mp.getUntilId() != null) {
                    builder.untilId(mp.getUntilId())
                }
                if (mp.getSinceId() != null) {
                    builder.sinceId(mp.getSinceId())
                }
            }
        }
    }

    // ============================================================== //
    // Classes
    // ============================================================== //
    // コメントに対してのコールバック設定
    internal class MisskeyCommentsListener(
        listener: EventCallback,
        service: Service,
        private val host: String
    ) : NoteCallback {
        private val listener: EventCallback = listener
        private val service: Service = service

        fun onNoteUpdate(note: Note?) {
            if (listener is UpdateCommentCallback) {
                val comment: Comment = MisskeyMapper.comment(note, host, service)
                (listener as UpdateCommentCallback).onUpdate(CommentEvent(comment))
            }
        }
    }

    // 通信に対してのコールバック設定
    internal class MisskeyConnectionListener(
        listener: EventCallback,
        runnable: java.lang.Runnable?
    ) : OpenedCallback, ClosedCallback,
        ErrorCallback {
        private val listener: EventCallback = listener
        private val runnable: java.lang.Runnable? = runnable

        fun onOpened() {
            if (listener is ConnectCallback) {
                (listener as ConnectCallback).onConnect()
            }
            if (runnable != null) {
                runnable.run()
            }
        }

        fun onClosed(remote: Boolean) {
            if (listener is DisconnectCallback) {
                (listener as DisconnectCallback).onDisconnect()
            }
        }

        fun onError(e: java.lang.Exception?) {
            logger.debug("WebSocket Error: ", e)
        }
    }

    // コメントに対してのコールバック設定
    internal class MisskeyNotificationListener(
        listener: EventCallback,
        emojis: List<Emoji>,
        service: Service,
        private val host: String,
        me: User
    ) : FollowedCallback, RenoteCallback, ReplayCallback, MentionCallback, NotificationCallback {
        private val listener: EventCallback = listener
        private val emojis: List<Emoji> = emojis
        private val service: Service = service
        private val me: User = me

        fun onFollowed(user: misskey4j.entity.User?) {
            if (listener is FollowUserCallback) {
                val model: User = MisskeyMapper.user(user, host, service)
                (listener as FollowUserCallback).onFollow(UserEvent(model))
            }
        }

        fun onMention(note: Note) {
            // Mention と Reply が重複することを防止
            // -> 自分に対する Reply の場合は Reply に移譲
            if (note.getReply().getUser().getId().equals(me.getId())) {
                return
            }

            if (listener is MentionCommentCallback) {
                val model: Comment = MisskeyMapper.comment(note, host, service)
                (listener as MentionCommentCallback).onMention(CommentEvent(model))
            }
        }

        fun onReply(note: Note?) {
            if (listener is MentionCommentCallback) {
                val model: Comment = MisskeyMapper.comment(note, host, service)
                (listener as MentionCommentCallback).onMention(CommentEvent(model))
            }
        }

        fun onNotification(notification: Notification) {
            // Reaction or Renote の場合のみ反応
            // (それ以外の場合は他でカバー済)

            if (notification.getType().equals("reaction") ||
                notification.getType().equals("renote")
            ) {
                if (listener is NotificationCommentCallback) {
                    val model: net.socialhub.core.model.Notification =
                        MisskeyMapper.notification(notification, emojis, host, service)
                    (listener as NotificationCommentCallback).onNotification(NotificationEvent(model))
                }
            }
        }

        fun onRenote(note: Note?) {
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