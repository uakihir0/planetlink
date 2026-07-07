package work.socialhub.planetlink.misskey.action


import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.kmisskey.Misskey
import work.socialhub.kmisskey.MisskeyException
import work.socialhub.kmisskey.api.model.PollRequest
import work.socialhub.kmisskey.api.request.blocks.BlocksCreateRequest
import work.socialhub.kmisskey.api.request.blocks.BlocksDeleteRequest
import work.socialhub.kmisskey.api.request.files.FilesCreateRequest
import work.socialhub.kmisskey.api.request.following.FollowingCreateRequest
import work.socialhub.kmisskey.api.request.following.FollowingDeleteRequest
import work.socialhub.kmisskey.api.request.hashtags.HashtagsTrendRequest
import work.socialhub.kmisskey.api.request.favorites.FavoritesCreateRequest
import work.socialhub.kmisskey.api.request.favorites.FavoritesDeleteRequest
import work.socialhub.kmisskey.api.request.i.IFavoritesRequest
import work.socialhub.kmisskey.api.request.i.INotificationsRequest
import work.socialhub.kmisskey.api.request.notifications.NotificationsMarkAllAsReadRequest
import work.socialhub.kmisskey.api.request.i.IRequest
import work.socialhub.kmisskey.api.request.i.IUpdateRequest
import work.socialhub.kmisskey.api.request.following.FollowingRequestsAcceptRequest
import work.socialhub.kmisskey.api.request.following.FollowingRequestsRejectRequest
import work.socialhub.kmisskey.api.request.lists.UsersListsCreateRequest
import work.socialhub.kmisskey.api.request.lists.UsersListsListRequest
import work.socialhub.kmisskey.api.request.lists.UsersListsPullRequest
import work.socialhub.kmisskey.api.request.lists.UsersListsPushRequest
import work.socialhub.kmisskey.api.request.lists.UsersListsShowRequest
import work.socialhub.kmisskey.api.request.meta.EmojisRequest
import work.socialhub.kmisskey.api.request.mutes.MutesCreateRequest
import work.socialhub.kmisskey.api.request.mutes.MutesDeleteRequest
import work.socialhub.kmisskey.api.request.notes.NoteUnrenoteRequest
import work.socialhub.kmisskey.api.request.notes.NotesChildrenRequest
import work.socialhub.kmisskey.api.request.notes.NotesConversationRequest
import work.socialhub.kmisskey.api.request.notes.NotesCreateRequest
import work.socialhub.kmisskey.api.request.notes.NotesDeleteRequest
import work.socialhub.kmisskey.api.request.notes.NotesFeaturedRequest
import work.socialhub.kmisskey.api.request.notes.NotesGlobalTimelineRequest
import work.socialhub.kmisskey.api.request.notes.NotesLocalTimelineRequest
import work.socialhub.kmisskey.api.request.notes.NotesSearchRequest
import work.socialhub.kmisskey.api.request.notes.NotesShowRequest
import work.socialhub.kmisskey.api.request.notes.NotesTimelineRequest
import work.socialhub.kmisskey.api.request.notes.NotesUserListTimelineRequest
import work.socialhub.kmisskey.api.request.notes.UsersNotesRequest
import work.socialhub.kmisskey.api.request.other.ServiceWorkerRegisterRequest
import work.socialhub.kmisskey.api.request.polls.PollsVoteRequest
import work.socialhub.kmisskey.api.request.protocol.PagingRequest
import work.socialhub.kmisskey.api.request.protocol.PagingTokenRequest
import work.socialhub.kmisskey.api.request.reactions.ReactionsCreateRequest
import work.socialhub.kmisskey.api.request.reactions.ReactionsDeleteRequest
import work.socialhub.kmisskey.api.request.users.UsersFollowersRequest
import work.socialhub.kmisskey.api.request.users.UsersFollowingsRequest
import work.socialhub.kmisskey.api.request.users.UsersRelationRequest
import work.socialhub.kmisskey.api.request.users.UsersReportAbuseRequest
import work.socialhub.kmisskey.api.request.users.UsersSearchRequest
import work.socialhub.kmisskey.api.request.users.UsersShowMultipleRequest
import work.socialhub.kmisskey.api.request.users.UsersShowSingleRequest
import work.socialhub.kmisskey.api.response.users.UsersShowResponse
import work.socialhub.kmisskey.entity.Note
import work.socialhub.kmisskey.entity.constant.NotificationType
import work.socialhub.kmisskey.stream.MisskeyStream
import work.socialhub.kmisskey.stream.callback.ClosedCallback
import work.socialhub.kmisskey.stream.callback.ErrorCallback as KmisskeyErrorCallback
import work.socialhub.kmisskey.stream.callback.FollowedCallback
import work.socialhub.kmisskey.stream.callback.MentionCallback
import work.socialhub.kmisskey.stream.callback.NotificationCallback
import work.socialhub.kmisskey.stream.callback.OpenedCallback
import work.socialhub.kmisskey.stream.callback.RenoteCallback
import work.socialhub.kmisskey.stream.callback.ReplayCallback
import work.socialhub.kmisskey.stream.callback.TimelineCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.MentionCommentCallback
import work.socialhub.planetlink.action.callback.comment.NotificationCommentCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.user.FollowUserCallback
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.StreamActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.misskey.define.MisskeyActionType
import work.socialhub.planetlink.misskey.define.MisskeyReactionType.Favorite
import work.socialhub.planetlink.misskey.define.MisskeyReactionType.Renote
import work.socialhub.planetlink.misskey.model.MisskeyPaging
import work.socialhub.planetlink.misskey.model.MisskeyPoll
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotImplementedException
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.utils.ExceptionHandler
import work.socialhub.planetlink.model.event.NotificationEvent
import work.socialhub.planetlink.model.event.UserEvent
import work.socialhub.planetlink.model.paging.OffsetPaging
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.ProfileForm
import kotlin.js.JsExport
import work.socialhub.kmisskey.entity.Notification as MNotification
import work.socialhub.kmisskey.entity.user.User as MUser

@JsExport
class MisskeyAction(
    account: Account,
    val auth: MisskeyAuth,
) : AccountActionImpl(account) {

    companion object {
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
                SocialActionType.GetNotification,
                SocialActionType.BookmarkComment,
                SocialActionType.UnbookmarkComment,
                SocialActionType.VotePoll,
                SocialActionType.ReportUser,
                SocialActionType.ReportComment,
                SocialActionType.UpdateProfile,
                SocialActionType.CreateList,
                SocialActionType.AddUserToList,
                SocialActionType.RemoveUserFromList,
                SocialActionType.AcceptFollowRequest,
                SocialActionType.RejectFollowRequest,
                SocialActionType.MarkNotificationsRead,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.MentionTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.UserLikeTimeLine,
                TimeLineActionType.UserMediaTimeLine,
                TimeLineActionType.SearchTimeLine,
                TimeLineActionType.ChannelTimeLine,

                UsersActionType.GetFollowingUsers,
                UsersActionType.GetFollowerUsers,
                UsersActionType.SearchUsers,

                StreamActionType.HomeTimeLineStream,
                StreamActionType.NotificationStream,

                MisskeyActionType.LocalTimeLine,
                MisskeyActionType.FederationTimeLine,
                MisskeyActionType.FeaturedTimeline,
            )
        )
    }

    override fun capabilities(): Capabilities = CAPABILITIES

    /** Actual instance hostname for emoji URL construction */
    private val instanceHost: String
        get() = (account.service.host ?: auth.host)
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .trimEnd('/')

    /** List of Emoji  */
    private var emojisCache: List<Emoji>? = null

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
     * Overrides the base `userMeWithCache()` and routes both it and `userMe()`
     * through this private function to avoid the Kotlin/JS yield* crash caused by
     * the unwired virtual suspend bridge for base→abstract `userMe()` delegation.
     * See AGENTS.md "Kotlin/JS yield* Bug".
     */
    private suspend fun fetchUserMe(): User {
        return proceed {
            val misskey = auth.accessor
            val response = misskey.accounts().i(IRequest())

            MisskeyMapper.user(
                response.data,
                instanceHost,
                service(),
            ).also { this.me = it }
        }
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

    // Free-standing impl so same-class callers (user(url)) don't route through
    // the unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun fetchUser(id: Identify): User {
        return proceed {
            val misskey = auth.accessor
            val user: UsersShowResponse
            val idv = id.id?.value

            // User のアカウント名で取得する場合
            if ((idv is String) && idv.startsWith("@")) {
                val elem = idv.split("@")
                val host = if ((elem.size > 2)) elem[2] else instanceHost

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
                instanceHost,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     * https://misskey.io/@syuilo
     * https://misskey.io/@syuilo@misskey.io
     */
    override suspend fun user(
        url: String
    ): User {
        val regex = ("https://(.+?)/@(.+)").toRegex()
        val matcher = regex.find(url)

        if (matcher != null) {
            val host = matcher.groupValues[1]
            val identify = matcher.groupValues[2]

            if (identify.contains("@")) {
                val format = ("@$identify")
                return fetchUser(Identify(service())
                    .also { it.id = ID(format) })
            } else {
                val format = ("@$identify@$host")
                return fetchUser(Identify(service())
                    .also { it.id = ID(format) })
            }
        }
        throw SocialHubException("this url is not supported format.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun followUser(
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
    override suspend fun unfollowUser(
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
    override suspend fun muteUser(
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
    override suspend fun unmuteUser(
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
    override suspend fun blockUser(
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
    override suspend fun unblockUser(
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
    override suspend fun relationship(
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

    /**
     * {@inheritDoc}
     */
    override suspend fun reportUser(
        id: Identify,
        comment: String?,
    ) {
        doReportAbuse(id.id<String>(), comment)
    }

    /**
     * {@inheritDoc}
     * Misskey の通報はユーザー単位。ノートの投稿者を解決して通報する。
     */
    override suspend fun reportComment(
        id: Identify,
        comment: String?,
    ) {
        proceedUnit {
            val misskey = auth.accessor
            // ノートを取得して投稿者の userId を解決
            val note = misskey.notes().show(
                NotesShowRequest().also {
                    it.noteId = id.id<String>()
                }).data

            misskey.users().reportAbuse(
                UsersReportAbuseRequest().also {
                    it.userId = note.user.id
                    it.comment = comment ?: ""
                })
        }
    }

    // Free-standing impl so same-class callers (reportUser) don't route through
    // the unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doReportAbuse(userId: String, comment: String?) {
        proceedUnit {
            auth.accessor.users().reportAbuse(
                UsersReportAbuseRequest().also {
                    it.userId = userId
                    it.comment = comment ?: ""
                })
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun updateProfile(
        form: ProfileForm
    ) {
        proceedUnit {
            val misskey = auth.accessor
            val request = IUpdateRequest()

            form.displayName?.let { request.name = it }
            form.description?.let { request.description = it }

            // アバター画像をアップロードして ID を設定
            form.avatar?.let { bytes ->
                request.avatarId = doUploadFile(bytes, form.avatarName)
            }
            // バナー画像をアップロードして ID を設定
            form.banner?.let { bytes ->
                request.bannerId = doUploadFile(bytes, form.bannerName)
            }

            misskey.accounts().iUpdate(request)
        }
    }

    // Free-standing upload helper (private -> safe from JS yield* bug).
    private suspend fun doUploadFile(
        bytes: ByteArray,
        name: String?,
    ): String {
        return auth.accessor.files().create(
            FilesCreateRequest().also {
                it.bytes = bytes
                it.name = name
                it.force = true
            }).data.id
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun acceptFollowRequest(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.following().acceptRequest(
                FollowingRequestsAcceptRequest().also {
                    it.userId = id.id<String>()
                })
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun rejectFollowRequest(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.following().rejectRequest(
                FollowingRequestsRejectRequest().also {
                    it.userId = id.id<String>()
                })
        }
    }

    /**
     * {@inheritDoc}
     * Misskey は全件既読のみ対応。upToId は無視する。
     */
    override suspend fun markNotificationsRead(
        upToId: Identify?
    ) {
        proceedUnit {
            auth.accessor.accounts().notificationsMarkAllAsRead(
                NotificationsMarkAllAsReadRequest())
        }
    }

    /**
     * {@inheritDoc}
     * Misskey のリスト作成は説明文 (description) 非対応のため無視する。
     */
    override suspend fun createList(
        name: String,
        description: String?,
    ): Channel {
        return proceed {
            val response = auth.accessor.lists().create(
                UsersListsCreateRequest(name))

            MisskeyMapper.channel(
                response.data,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun addUserToList(
        channel: Identify,
        user: Identify,
    ) {
        proceedUnit {
            auth.accessor.lists().push(
                UsersListsPushRequest().also {
                    it.listId = channel.id<String>()
                    it.userId = user.id<String>()
                })
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun removeUserFromList(
        channel: Identify,
        user: Identify,
    ) {
        proceedUnit {
            auth.accessor.lists().pull(
                UsersListsPullRequest().also {
                    it.listId = channel.id<String>()
                    it.userId = user.id<String>()
                })
        }
    }

    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //

    /**
     * {@inheritDoc}
     */
    override suspend fun followingUsers(
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
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun followerUsers(
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
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun searchUsers(
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
                instanceHost,
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
    override suspend fun homeTimeLine(
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
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun mentionTimeLine(
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
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userCommentTimeLine(
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
                instanceHost,
                service(),
                paging
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userLikeTimeLine(
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
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userMediaTimeLine(
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
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun searchTimeLine(
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
                instanceHost,
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
    override suspend fun postComment(
        req: CommentForm
    ) {
        if (req.isMessage) {
            // Misskey has no message API; inline the default postMessage() behavior
            // instead of calling it (same-class suspend call -> JS yield* bug).
            throw NotImplementedException()
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
                            it.comment = image.description
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
    override suspend fun comment(
        id: Identify
    ): Comment {
        return fetchComment(id)
    }

    // Free-standing impl so same-class callers (comment(url)) don't route through
    // the unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun fetchComment(id: Identify): Comment {
        return proceed {
            val misskey = auth.accessor
            val response = misskey.notes().show(
                NotesShowRequest().also {
                    it.noteId = id.id<String>()
                })

            MisskeyMapper.comment(
                response.data,
                instanceHost,
                service()
            )
        }
    }

    /**
     * {@inheritDoc}
     * Parse Misskey Post's url, like:
     * https://misskey.io/notes/8axwbcxiff
     */
    override suspend fun comment(
        url: String
    ): Comment {
        val regex = ("https://(.+?)/notes/(.+)").toRegex()
        val matcher = regex.find(url)

        if (matcher != null) {
            return fetchComment(
                Identify(service()).also {
                    it.id = ID(matcher.groupValues[2])
                })
        }

        throw SocialHubException("this url is not supported format.")
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
        proceedUnit {
            auth.accessor.reactions().create(
                ReactionsCreateRequest().also {
                    it.noteId = id.id<String>()
                    it.reaction = "❤"
                })
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
        proceedUnit {
            auth.accessor.reactions().delete(
                ReactionsDeleteRequest().also {
                    it.noteId = id.id<String>()
                })
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
    override suspend fun unshareComment(
        id: Identify
    ) {
        doUnshareComment(id)
    }

    private suspend fun doUnshareComment(id: Identify) {
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
    override suspend fun reactionComment(
        id: Identify,
        reaction: String,
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (Favorite.codes.contains(type)) {
                doLikeComment(id)
                return
            }
            if (Renote.codes.contains(type)) {
                doShareComment(id)
                return
            }

            proceedUnit {
                auth.accessor.reactions().create(
                    ReactionsCreateRequest().also {
                        it.noteId = id.id<String>()
                        it.reaction = reaction
                    })
            }
            return
        }

        throw SocialHubException("not supported.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unreactionComment(
        id: Identify,
        reaction: String,
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (Favorite.codes.contains(type)) {
                doUnlikeComment(id)
                return
            }
            if (Renote.codes.contains(type)) {
                doUnshareComment(id)
                return
            }

            // ユーザーごとにリアクションは一つのみ
            proceedUnit {
                auth.accessor.reactions().delete(
                    ReactionsDeleteRequest().also {
                        it.noteId = id.id<String>()
                    })
            }
            return
        }

        throw SocialHubException("not supported.")
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun deleteComment(
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
     * {@inheritDoc}
     */
    override fun emojis(): List<Emoji> {
        return emojisCache ?: super.emojis()
    }

    /**
     * Get List of Emojis
     */
    suspend fun getEmojis(): List<Emoji> {
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
    override suspend fun commentContexts(
        id: Identify
    ): Context {
        return commentContext(id)
    }

    /**
     * {@inheritDoc}
     */
    suspend fun commentContext(
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
                .map { MisskeyMapper.comment(it, instanceHost, service()) }
            context.descendants = descendants
                .map { MisskeyMapper.comment(it, instanceHost, service()) }

            // 並び替えを実行
            context.sort()
            context
        }
    }

    // さらなる返信を探して notes に追加
    private suspend fun moreReplies(
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
    override suspend fun channels(
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
    override suspend fun channelTimeLine(
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
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun channelUsers(
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
                instanceHost,
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
    override suspend fun votePoll(
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
    // Bookmark (Favorites)
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun bookmarkComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.favorites().create(
                FavoritesCreateRequest().also {
                    it.noteId = id.id<String>()
                }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unbookmarkComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.favorites().delete(
                FavoritesDeleteRequest().also {
                    it.noteId = id.id<String>()
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
    suspend fun trends(
        limit: Int
    ): List<Trend> {
        return proceed {
            val response = auth.accessor.hashtags().trend(
                HashtagsTrendRequest()
            )

            response.data.take(limit).map {
                MisskeyMapper.trend(it)
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun notification(
        paging: Paging
    ): Pageable<Notification> {
        // Fetch emojis outside proceed to avoid broken virtual suspend bridge
        val emojis = this.getEmojis()

        return proceed {
            val misskey = auth.accessor

            val builder = INotificationsRequest()
            setPaging(builder, paging)

            builder.markAsRead = true
            builder.includeTypes = arrayOf(
                NotificationType.FOLLOW.code,
                NotificationType.REACTION.code,
                NotificationType.RENOTE.code,
                NotificationType.POLL_ENDED.code
            )

            val response = misskey.accounts()
                .iNotifications(builder)

            MisskeyMapper.notifications(
                response.data.toList(),
                emojis,
                instanceHost,
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
    suspend fun homeTimeLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey, account.service.streamHost)

            val commentsListener = MisskeyCommentsListener(
                callback,
                service(),
                instanceHost,
            )

            val connectionListener = MisskeyConnectionListener(callback) {
                stream.homeTimeLine(commentsListener)
            }

            setStreamConnectionCallback(stream, connectionListener)
            work.socialhub.planetlink.misskey.model.MisskeyStream(stream)
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun setHomeTimeLineStream(
        callback: EventCallback
    ): Stream {
        return homeTimeLineStream(callback)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun setNotificationStream(
        callback: EventCallback
    ): Stream {
        return notificationStream(callback)
    }

    /**
     * {@inheritDoc}
     */
    suspend fun notificationStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey, account.service.streamHost)

            val notificationListener = MisskeyNotificationListener(
                callback,
                getEmojis(),
                service(),
                instanceHost,
                userMeWithCache()
            )

            val connectionListener = MisskeyConnectionListener(callback) {
                stream.main(notificationListener)
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
    suspend fun localTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val misskey = auth.accessor
            val request = NotesLocalTimelineRequest()

            setPaging(request, paging)
            val response = misskey.notes().localTimeline(request)

            MisskeyMapper.timeLine(
                response.data.toList(),
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    suspend fun federationTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val misskey = auth.accessor
            val request = NotesGlobalTimelineRequest()

            setPaging(request, paging)
            val response = misskey.notes().globalTimeline(request)

            MisskeyMapper.timeLine(
                response.data.toList(),
                instanceHost,
                service(),
                paging,
            )
        }
    }

    /**
     * Get Featured Timeline
     */
    suspend fun featuredTimeLine(
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
                instanceHost,
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
    suspend fun localLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey, account.service.streamHost)

            val commentsListener = MisskeyCommentsListener(
                callback,
                service(),
                instanceHost,
            )

            val connectionListener = MisskeyConnectionListener(callback) {
                stream.localTimeline(commentsListener)
            }
            setStreamConnectionCallback(stream, connectionListener)
            work.socialhub.planetlink.misskey.model.MisskeyStream(stream)
        }
    }

    /**
     * {@inheritDoc}
     */
    suspend fun federationLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val misskey = auth.accessor
            val stream = MisskeyStream(misskey, account.service.streamHost)

            val commentsListener = MisskeyCommentsListener(
                callback,
                service(),
                instanceHost,
            )
            val connectionListener = MisskeyConnectionListener(callback) {
                stream.globalTimeline(commentsListener)
            }

            setStreamConnectionCallback(stream, connectionListener)
            work.socialhub.planetlink.misskey.model.MisskeyStream(stream)
        }
    }

    /**
     * Register ServiceWorker endpoint.
     * サービスワーカーのエンドポイントを設定
     */
    suspend fun registerSubscription(
        endpoint: String,
        publicKey: String,
        authSecret: String
    ) {
        proceedUnit {
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
        val runnable: suspend () -> Unit
    ) : OpenedCallback,
        ClosedCallback,
        KmisskeyErrorCallback {

        override fun onOpened() {
            if (listener is ConnectCallback) {
                listener.onConnect()
            }
            CoroutineScope(Dispatchers.Default).launch {
                runnable()
            }
        }

        override fun onClosed() {
            if (listener is DisconnectCallback) {
                listener.onDisconnect()
            }
        }

        override fun onError(e: Exception) {
            if (listener is work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback) {
                val classified = if (e is SocialHubException) e
                    else ExceptionHandler.classify(e, ServiceType.Misskey,
                        statusCode = (e as? MisskeyException)?.status,
                        responseBody = (e as? MisskeyException)?.body)
                listener.onError(classified)
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
    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Misskey,
            statusExtractor = { e -> (e as? MisskeyException)?.status },
            bodyExtractor = { e -> (e as? MisskeyException)?.body },
            runner = runner,
        )
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Misskey,
            statusExtractor = { e -> (e as? MisskeyException)?.status },
            bodyExtractor = { e -> (e as? MisskeyException)?.body },
            runner = runner,
        )
    }
}