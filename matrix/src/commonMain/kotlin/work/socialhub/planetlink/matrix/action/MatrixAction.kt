package work.socialhub.planetlink.matrix.action

import kotlin.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import work.socialhub.kmatrix.api.request.events.EventsGetContextRequest
import work.socialhub.kmatrix.api.request.relations.RelationsGetRequest
import work.socialhub.kmatrix.api.request.media.MediaDownloadRequest
import work.socialhub.kmatrix.api.request.media.MediaThumbnailRequest
import work.socialhub.kmatrix.api.request.notifications.NotificationsGetRequest
import work.socialhub.kmatrix.api.request.rooms.RoomsGetMessagesRequest
import work.socialhub.kmatrix.api.request.rooms.RoomsRedactEventRequest
import work.socialhub.kmatrix.api.request.rooms.RoomsSendMessageRequest
import work.socialhub.kmatrix.api.request.rooms.RoomsSetReadMarkersRequest
import work.socialhub.kmatrix.api.request.sync.SyncRequest
import work.socialhub.kmatrix.api.request.userdirectory.UserDirectorySearchRequest
import work.socialhub.kmatrix.api.response.events.EventsGetContextResponse
import work.socialhub.kmatrix.api.response.events.EventsGetEventResponse
import work.socialhub.kmatrix.api.response.notifications.NotificationsGetResponse
import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.kmatrix.stream.MatrixStreamFactory
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.StreamActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.kmatrix.MatrixException
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.utils.ExceptionHandler
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.matrix.model.MatrixComment
import work.socialhub.planetlink.matrix.model.MatrixPaging
import work.socialhub.planetlink.matrix.model.MatrixSpace
import work.socialhub.planetlink.matrix.model.MatrixUser
import kotlin.js.JsExport
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@JsExport
class MatrixAction(
    account: Account,
    val auth: MatrixAuth,
) : AccountActionImpl(account) {

    companion object {
        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.GetComment,
                SocialActionType.GetContext,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.ReactionComment,
                SocialActionType.UnreactionComment,
                SocialActionType.GetSpaces,
                SocialActionType.GetChannels,
                SocialActionType.GetNotification,

                TimeLineActionType.MentionTimeLine,
                TimeLineActionType.ChannelTimeLine,

                UsersActionType.SearchUsers,
                UsersActionType.ChannelUsers,

                MessageActionType.GetMessageThread,
                MessageActionType.GetMessageTimeLine,
                MessageActionType.PostMessage,

                StreamActionType.HomeTimeLineStream,
                StreamActionType.NotificationStream,
            )
        )

        /**
         * Time-to-live for the cached room snapshot. A `spaces()` call followed
         * by `channels(space)` within this window reuses one `/sync`.
         */
        private val SNAPSHOT_TTL = 15.seconds

        /**
         * Inline `/sync` filter: only the room state we need to build the
         * space / channel / DM lists (creation type, name, topic, avatar,
         * canonical alias, space children/parent, membership) plus the
         * top-level `m.direct` account data. `timeout=0` returns immediately.
         *
         * `lazy_load_members=true` limits `m.room.member` events to the room's
         * heroes / timeline senders; room names then come from the server
         * `summary` (heroes + member counts), so large public rooms no longer
         * dump their full member list. See [MatrixSnapshotParser].
         */
        private const val SNAPSHOT_FILTER_JSON =
            "{\"room\":{\"timeline\":{\"limit\":1}," +
                "\"state\":{\"lazy_load_members\":true,\"types\":[" +
                "\"m.room.create\",\"m.room.name\",\"m.room.topic\",\"m.room.avatar\"," +
                "\"m.room.canonical_alias\",\"m.space.child\",\"m.space.parent\"," +
                "\"m.room.member\"]}," +
                "\"ephemeral\":{\"types\":[]},\"account_data\":{\"types\":[]}}," +
                "\"presence\":{\"types\":[]}," +
                "\"account_data\":{\"types\":[\"m.direct\"]}}"
    }

    override fun capabilities(): Capabilities = CAPABILITIES

    private val accessor get() = auth.accessor

    /** Last parsed sync snapshot and when it was taken (for the TTL cache). */
    private var snapshotCache: MatrixSnapshot? = null
    private var snapshotMark: TimeSource.Monotonic.ValueTimeMark? = null

    /**
     * Fetch (or reuse within [SNAPSHOT_TTL]) a single `/sync` snapshot of all
     * joined rooms, their state and the DM map. This is the one network call
     * that backs [spaces], [channels] and [messageThread] — no per-room lookups.
     * Private + free-standing so same-class callers avoid the Kotlin/JS yield*
     * bridge crash. See AGENTS.md "Kotlin/JS yield* Bug".
     */
    private suspend fun loadRoomSnapshot(force: Boolean = false): MatrixSnapshot {
        val cached = snapshotCache
        val fresh = snapshotMark?.let { it.elapsedNow() < SNAPSHOT_TTL } == true
        if (!force && cached != null && fresh) return cached

        return proceed {
            val selfUserId = (userMeWithCache() as? MatrixUser)?.userId ?: ""
            val sync = accessor.sync().sync(
                SyncRequest().apply {
                    filter = SNAPSHOT_FILTER_JSON
                    timeout = 0
                }
            ).data
            MatrixSnapshotParser.parse(sync, selfUserId).also {
                snapshotCache = it
                snapshotMark = TimeSource.Monotonic.markNow()
            }
        }
    }

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
            val whoami = accessor.accounts().whoami().data
            val profile = accessor.profile().getProfile(whoami.userId).data
            val user = MatrixMapper.user(whoami.userId, profile, service())
            me = user
            user
        }
    }

    override suspend fun userMeWithCache(): User {
        return me ?: fetchUserMe()
    }

    override suspend fun user(id: Identify): User {
        val key = id.id!!.value<String>()
        if (key == (me as? MatrixUser)?.userId && me != null) return me!!

        return proceed {
            val profile = accessor.profile().getProfile(key).data
            MatrixMapper.user(key, profile, service())
        }
    }

    override suspend fun user(url: String): User {
        return proceed {
            val trimmed = url.trim()
            if (trimmed.startsWith("@") && trimmed.contains(":")) {
                val profile = accessor.profile().getProfile(trimmed).data
                MatrixMapper.user(trimmed, profile, service())
            } else {
                throw SocialHubException("Invalid Matrix user URL: $url")
            }
        }
    }

    override suspend fun followUser(id: Identify) {
        throw NotSupportedException("Matrix does not support following users")
    }

    override suspend fun unfollowUser(id: Identify) {
        throw NotSupportedException("Matrix does not support unfollowing users")
    }

    override suspend fun muteUser(id: Identify) {
        throw NotSupportedException("Matrix does not support muting users")
    }

    override suspend fun unmuteUser(id: Identify) {
        throw NotSupportedException("Matrix does not support unmuting users")
    }

    override suspend fun blockUser(id: Identify) {
        throw NotSupportedException("Matrix does not support blocking users")
    }

    override suspend fun unblockUser(id: Identify) {
        throw NotSupportedException("Matrix does not support unblocking users")
    }

    override suspend fun relationship(id: Identify): Relationship {
        throw NotSupportedException("Matrix does not support relationships")
    }

    override suspend fun followingUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException("Matrix does not support following users")
    }

    override suspend fun followerUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException("Matrix does not have follower functionality")
    }

    override suspend fun searchUsers(query: String, paging: Paging): Pageable<User> {
        return proceed {
            val response = accessor.userDirectory().search(
                UserDirectorySearchRequest().apply {
                    searchTerm = query
                    limit = paging.count ?: 50
                }
            ).data

            val users = response.results.map { u ->
                MatrixMapper.user(u.userId, u.displayName, u.avatarUrl, service())
            }
            Pageable<User>().also {
                it.entities = users
                it.paging = paging
            }
        }
    }

    override suspend fun homeTimeLine(paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Matrix does not support home timeline")
    }

    override suspend fun mentionTimeLine(paging: Paging): Pageable<Comment> {
        return proceed {
            val response = accessor.notifications().getNotifications(
                NotificationsGetRequest().apply {
                    limit = paging.count ?: 50
                }
            ).data

            val events = response.notifications
                .filter { it.event.type == "m.room.message" }
                .map { it.event.toRoomEvent() }
                .filterNotNull()

            val userMe = userMeWithCache()
            MatrixMapper.timeLine(events, service(), paging, userMe)
        }
    }

    override suspend fun notification(paging: Paging): Pageable<Notification> {
        return proceed {
            val response = accessor.notifications().getNotifications(
                NotificationsGetRequest().apply {
                    limit = paging.count ?: 50
                }
            ).data

            MatrixMapper.notifications(response.notifications, service(), paging)
        }
    }

    override suspend fun userCommentTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Matrix does not support user comment timeline")
    }

    override suspend fun userLikeTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Matrix does not support like timeline")
    }

    override suspend fun userMediaTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Matrix does not support media timeline")
    }

    override suspend fun searchTimeLine(query: String, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Matrix search is not yet supported")
    }

    override suspend fun postComment(req: CommentForm) {
        doPostComment(req)
    }

    // Free-standing impls so same-class callers (postMessage, likeComment, etc.)
    // don't route through the unwired JS virtual suspend bridge.
    // See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doPostComment(req: CommentForm) {
        proceedUnit {
            val roomId = req.params[MatrixComment.ROOM_KEY] as? String
                ?: throw SocialHubException("Room ID is required for Matrix messages")

            accessor.rooms().sendMessage(
                RoomsSendMessageRequest().apply {
                    this.roomId = roomId
                    body = req.text ?: ""
                    msgtype = "m.text"
                    replyTo = req.replyId?.value<String>()
                }
            )
        }
    }

    override suspend fun comment(id: Identify): Comment {
        val matrixComment = id as? MatrixComment
            ?: throw SocialHubException("Matrix comment requires roomId and eventId")

        return proceed {
            val response = accessor.events().getEvent(
                matrixComment.roomId ?: throw SocialHubException("Room ID is required"),
                matrixComment.eventId ?: throw SocialHubException("Event ID is required"),
            ).data

            val userMe = userMeWithCache()
            val roomEvent = response.toRoomEvent()
            MatrixMapper.comment(roomEvent, service(), userMe)
                ?: throw SocialHubException("Could not parse comment")
        }
    }

    override suspend fun comment(url: String): Comment {
        return proceed {
            val parsed = parseMatrixPermalink(url)
            val response = accessor.events().getEvent(parsed.roomId, parsed.eventId).data
            val userMe = userMeWithCache()
            val roomEvent = response.toRoomEvent()
            MatrixMapper.comment(roomEvent, service(), userMe)
                ?: throw SocialHubException("Could not parse comment")
        }
    }

    override suspend fun likeComment(id: Identify) {
        doReactionComment(id, "\uD83D\uDC4D")
    }

    override suspend fun unlikeComment(id: Identify) {
        doUnreactionComment(id, "\uD83D\uDC4D")
    }

    override suspend fun shareComment(id: Identify) {
        throw NotSupportedException("Matrix does not support sharing comments")
    }

    override suspend fun unshareComment(id: Identify) {
        throw NotSupportedException("Matrix does not support unsharing comments")
    }

    override suspend fun reactionComment(id: Identify, reaction: String) {
        doReactionComment(id, reaction)
    }

    private suspend fun doReactionComment(id: Identify, reaction: String) {
        proceedUnit {
            val comment = id as? MatrixComment
                ?: throw SocialHubException("Not a Matrix comment")

            accessor.rooms().sendMessage(
                RoomsSendMessageRequest().apply {
                    roomId = comment.roomId
                    body = reaction
                    msgtype = "m.reaction"
                    relatesToType = "m.annotation"
                    relatesToEventId = comment.eventId
                    relatesToKey = reaction
                }
            )
        }
    }

    override suspend fun unreactionComment(id: Identify, reaction: String) {
        doUnreactionComment(id, reaction)
    }

    private suspend fun doUnreactionComment(id: Identify, reaction: String) {
        val selfUserId = ((me ?: fetchUserMe()) as? MatrixUser)?.userId
            ?: throw SocialHubException("Could not resolve current Matrix user")

        proceedUnit {
            val comment = id as? MatrixComment
                ?: throw SocialHubException("Not a Matrix comment")

            var from: String? = null
            val reactionEventIds = mutableListOf<String>()

            do {
                val response = accessor.relations().getRelations(
                    RelationsGetRequest().apply {
                        roomId = comment.roomId
                        eventId = comment.eventId
                        relType = "m.annotation"
                        eventType = "m.reaction"
                        this.from = from
                        limit = 100
                        dir = "b"
                    }
                ).data

                reactionEventIds.addAll(
                    response.chunk
                        .filter { event ->
                            event.sender == selfUserId &&
                                matrixReactionKey(event.content) == reaction
                        }
                        .mapNotNull { it.eventId }
                )
                from = response.nextBatch
            } while (from != null)

            reactionEventIds.forEach { eid ->
                accessor.rooms().redactEvent(
                    RoomsRedactEventRequest().apply {
                        roomId = comment.roomId
                        eventId = eid
                    }
                )
            }
        }
    }

    override suspend fun deleteComment(id: Identify) {
        proceedUnit {
            val comment = id as? MatrixComment
                ?: throw SocialHubException("Not a Matrix comment")

            accessor.rooms().redactEvent(
                RoomsRedactEventRequest().apply {
                    roomId = comment.roomId
                    eventId = comment.eventId
                }
            )
        }
    }

    /**
     * Matrix-specific extra: mark a room as read up to [eventId].
     * fully-read マーカーと公開/非公開の既読レシートを [eventId] に設定する。
     * 統一 AccountAction 外のため capability には登録しない。
     */
    suspend fun markRoomRead(room: Identify, eventId: String) {
        proceedUnit {
            val roomId = room.id!!.value<String>()
            accessor.rooms().setReadMarkers(
                RoomsSetReadMarkersRequest().apply {
                    this.roomId = roomId
                    fullyRead = eventId
                    read = eventId
                    readPrivate = eventId
                }
            )
        }
    }

    /**
     * Matrix-specific extra: turn an `mxc://{server}/{mediaId}` content URI into
     * an HTTP(S) URL an `<img>` can load directly, using this account's
     * homeserver as the base. Targets the legacy **unauthenticated** media
     * endpoints (`/_matrix/media/v3/download` and `/thumbnail`), which need no
     * `Authorization` header — so unlike [resolveMedia] the caller renders it
     * with a plain `src` and no byte fetch / blob wrapping.
     *
     * Prefer this for avatars and inline images on homeservers that still serve
     * v3 (e.g. matrix.org). On servers that have frozen v3 the URL will error;
     * fall back to [resolveMedia] (authenticated bytes) there. Returns null for a
     * non-mxc / empty input.
     *
     * A [width]/[height] (both required) yields a scaled thumbnail URL; omitting
     * them yields the full download URL.
     *
     * 統一 AccountAction 外のため capability には登録しない。
     */
    fun mxcToHttpUrl(
        mxcUrl: String?,
        width: Int? = null,
        height: Int? = null,
    ): String? {
        return MatrixMapper.mxcToHttpUrl(mxcUrl, auth.host, width, height)
    }

    /**
     * Matrix-specific extra: resolve an `mxc://{server}/{mediaId}` content URI
     * to raw bytes via the (authenticated) media API, so callers can render it
     * (e.g. as a blob/data URL). Use this on homeservers that require
     * authenticated media (Matrix 1.11 / MSC3916) and have frozen the legacy
     * unauthenticated endpoints; otherwise [mxcToHttpUrl] is simpler.
     *
     * When [width] and [height] are both provided a scaled thumbnail is fetched;
     * otherwise the full-resolution file is downloaded. The unified url fields
     * (`User.iconImageUrl`, `Space.iconUrl`, `Media.sourceUrl` / `previewUrl`)
     * are normalised to HTTP by the mappers; pass instead the retained raw mxc —
     * `MatrixUser.avatarUrl` or `MatrixMedia.sourceMxcUrl` / `previewMxcUrl` —
     * when the unauthenticated HTTP URL is rejected and you need authenticated
     * bytes.
     *
     * 統一 AccountAction 外のため capability には登録しない。
     */
    suspend fun resolveMedia(
        mxcUrl: String,
        width: Int? = null,
        height: Int? = null,
    ): ByteArray {
        return proceed {
            val parts = mxcUrl.removePrefix("mxc://").split("/", limit = 2)
            val serverName = parts.getOrNull(0)
                ?.takeIf { it.isNotEmpty() }
                ?: throw SocialHubException("Invalid mxc URI: $mxcUrl")
            val mediaId = parts.getOrNull(1)
                ?.takeIf { it.isNotEmpty() }
                ?: throw SocialHubException("Invalid mxc URI: $mxcUrl")

            if (width != null && height != null) {
                accessor.media().thumbnail(
                    MediaThumbnailRequest().apply {
                        this.serverName = serverName
                        this.mediaId = mediaId
                        this.width = width
                        this.height = height
                        this.method = "scale"
                    }
                )
            } else {
                accessor.media().download(
                    MediaDownloadRequest().apply {
                        this.serverName = serverName
                        this.mediaId = mediaId
                    }
                )
            }
        }
    }

    override suspend fun commentContexts(id: Identify): Context {
        return proceed {
            val comment = id as? MatrixComment
                ?: throw SocialHubException("Not a Matrix comment")

            val response = accessor.events().getContext(
                EventsGetContextRequest().apply {
                    roomId = comment.roomId
                    eventId = comment.eventId
                    limit = 100
                }
            ).data

            val userMe = userMeWithCache()
            val context = Context()
            context.ancestors = response.eventsBefore.mapNotNull { ce ->
                ce.toMatrixComment(service(), userMe)
            }
            context.descendants = response.eventsAfter.mapNotNull { ce ->
                ce.toMatrixComment(service(), userMe)
            }
            context.sort()
            context
        }
    }

    override suspend fun spaces(paging: Paging): Pageable<Space> {
        return doSpaces(paging)
    }

    private suspend fun doSpaces(paging: Paging): Pageable<Space> {
        val snapshot = loadRoomSnapshot()
        val spaces = snapshot.rooms.values.filter { it.isSpace }
        return MatrixMapper.spaces(spaces, service(), paging)
    }

    override suspend fun channels(id: Identify, paging: Paging): Pageable<Channel> {
        return doChannels(id, paging)
    }

    private suspend fun doChannels(id: Identify, paging: Paging): Pageable<Channel> {
        val snapshot = loadRoomSnapshot()

        // Dispatch: a MatrixSpace (or a room id that the snapshot marks as a
        // space) returns that space's direct child channels; anything else
        // (a user/account identify, or a non-space room id) returns the flat
        // list of joined, non-space rooms.
        val spaceRoomId = when {
            id is MatrixSpace -> id.roomId ?: id.id?.value<String>()
            else -> id.id?.value<String>()?.takeIf { snapshot.rooms[it]?.isSpace == true }
        }

        val summaries = if (spaceRoomId != null) {
            val children = snapshot.rooms[spaceRoomId]?.childRoomIds ?: emptyList()
            children.map { childId ->
                // Unjoined children aren't in the snapshot; fall back to the id
                // as the name rather than issuing a per-room lookup.
                snapshot.rooms[childId] ?: MatrixRoomSummary(
                    roomId = childId,
                    displayName = childId,
                    topic = null,
                    avatarUrl = null,
                    createAtMs = null,
                    isSpace = false,
                    isDirect = false,
                    childRoomIds = emptyList(),
                    memberCount = 0,
                )
            }
        } else {
            snapshot.rooms.values.filter { !it.isSpace }
        }

        return MatrixMapper.channels(summaries, service(), paging)
    }

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return doChannelTimeLine(id, paging)
    }

    private suspend fun doChannelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return proceed {
            val roomId = id.id!!.value<String>()
            val mp = MatrixPaging.fromPaging(paging)
            val response = accessor.rooms().getMessages(
                MatrixMapper.createGetMessagesRequest(roomId, mp, paging.count ?: 50)
            ).data

            // The lazy_load_members filter makes the server return this chunk's
            // senders' m.room.member state, so each message's display name /
            // avatar resolves without a per-sender profile lookup.
            val members = MatrixMapper.memberInfoMap(response.state?.toList() ?: emptyList())

            val userMe = userMeWithCache()
            val pageable = MatrixMapper.timeLine(
                response.chunk.toList(),
                service(),
                paging,
                userMe,
                members,
            )
            pageable.paging = (pageable.paging as? MatrixPaging)?.apply {
                from = response.start
                to = response.end
            } ?: pageable.paging
            pageable
        }
    }

    override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
        return proceed {
            val roomId = id.id!!.value<String>()
            val response = accessor.rooms().getJoinedMembers(roomId).data
            MatrixMapper.usersToPageable(response, service(), paging)
        }
    }

    override suspend fun messageThread(paging: Paging): Pageable<Thread> {
        return doMessageThread(paging)
    }

    private suspend fun doMessageThread(paging: Paging): Pageable<Thread> {
        val snapshot = loadRoomSnapshot()
        val dmRooms = snapshot.rooms.values.filter { it.isDirect }
        return MatrixMapper.threads(dmRooms, service(), paging)
    }

    override suspend fun messageTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return doChannelTimeLine(id, paging)
    }

    override suspend fun postMessage(req: CommentForm) {
        doPostComment(req)
    }

    override suspend fun setHomeTimeLineStream(callback: EventCallback): Stream {
        return doSetHomeTimeLineStream(callback)
    }

    private suspend fun doSetHomeTimeLineStream(callback: EventCallback): Stream {
        return proceed {
            val kmatrixStream = MatrixStreamFactory.instance(
                auth.host,
                auth.accessToken ?: ""
            )
            val stream = MatrixStream(kmatrixStream, callback)
            stream.open()
            stream
        }
    }

    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        return doSetHomeTimeLineStream(callback)
    }

    override fun request(): RequestAction {
        return MatrixRequest(account)
    }

    private fun service(): Service = account.service

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Matrix,
            statusExtractor = { e -> (e as? MatrixException)?.status },
            bodyExtractor = { e -> (e as? MatrixException)?.body },
            runner = runner,
        )
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Matrix,
            statusExtractor = { e -> (e as? MatrixException)?.status },
            bodyExtractor = { e -> (e as? MatrixException)?.body },
            runner = runner,
        )
    }

    private data class Permalink(val roomId: String, val eventId: String)

    private fun parseMatrixPermalink(url: String): Permalink {
        val trimmed = url.trim()
        val regex = Regex("https?://matrix\\.to/#/([^/]+)/([^/]+)")
        val match = regex.find(trimmed)
        if (match != null) {
            return Permalink(match.groupValues[1], match.groupValues[2])
        }
        throw SocialHubException("Invalid Matrix URL format: $url")
    }
}

private fun EventsGetEventResponse.toRoomEvent(): RoomEvent {
    return RoomEvent().apply {
        type = this@toRoomEvent.type
        eventId = this@toRoomEvent.eventId
        sender = this@toRoomEvent.sender
        originServerTs = this@toRoomEvent.originServerTs
        roomId = this@toRoomEvent.roomId
        content = this@toRoomEvent.content.mapValues { (_, v) ->
            extractJsonValue(v)
        }
    }
}

private fun NotificationsGetResponse.Event.toRoomEvent(): RoomEvent? {
    if (type != "m.room.message") return null
    return RoomEvent().apply {
        this.type = this@toRoomEvent.type
        eventId = this@toRoomEvent.eventId
        sender = this@toRoomEvent.sender
        originServerTs = this@toRoomEvent.originServerTs
        roomId = this@toRoomEvent.roomId
        content = this@toRoomEvent.content.mapValues { (_, v) ->
            extractJsonValue(v)
        }
    }
}

private fun EventsGetContextResponse.ContextEvent.toMatrixComment(
    service: Service,
    userMe: User?,
): MatrixComment? {
    if (type != "m.room.message") return null
    val body = getString(content, "body") ?: return null
    val msgtype = getString(content, "msgtype") ?: "m.text"
    val url = getString(content, "url")

    return MatrixComment(service).apply {
        eventId = this@toMatrixComment.eventId
        roomId = this@toMatrixComment.roomId
        id = ID(this@toMatrixComment.eventId)
        createAt = Instant.fromEpochMilliseconds(this@toMatrixComment.originServerTs)
        this.msgtype = msgtype
        text = AttributedString.plain(body)
        if (url != null && (msgtype == "m.image" || msgtype == "m.file" || msgtype == "m.video")) {
            medias = listOf(
                work.socialhub.planetlink.model.Media().apply {
                    sourceUrl = url
                    previewUrl = url
                    type = when (msgtype) {
                        "m.image" -> work.socialhub.planetlink.define.MediaType.Image
                        "m.video" -> work.socialhub.planetlink.define.MediaType.Movie
                        else -> work.socialhub.planetlink.define.MediaType.File
                    }
                }
            )
        }
    }
}

private fun getString(content: Map<String, JsonElement>, key: String): String? {
    return content[key]?.jsonPrimitive?.contentOrNull
}

internal fun matrixReactionKey(content: Map<String, JsonElement>): String? {
    val relatesTo = content["m.relates_to"] as? JsonObject ?: return null
    return getString(relatesTo, "key")
}

private fun extractJsonValue(element: JsonElement): Any? {
    return when {
        element.jsonPrimitive.isString -> element.jsonPrimitive.content
        element.jsonPrimitive.contentOrNull != null -> element.jsonPrimitive.content
        else -> null
    }
}
