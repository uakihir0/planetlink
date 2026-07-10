package work.socialhub.planetlink.matrix.action

import kotlin.time.Instant
import work.socialhub.kmatrix.api.request.rooms.RoomsGetMessagesRequest
import work.socialhub.kmatrix.api.response.notifications.NotificationsGetResponse
import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.kmatrix.api.response.rooms.RoomsGetJoinedMembersResponse
import work.socialhub.kmatrix.api.response.profile.ProfileGetProfileResponse
import work.socialhub.planetlink.define.AttributedType as AttributedTypeDef
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Space
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.matrix.model.MatrixComment
import work.socialhub.planetlink.matrix.model.MatrixPaging
import work.socialhub.planetlink.matrix.model.MatrixSpace
import work.socialhub.planetlink.matrix.model.MatrixUser

private val MATRIX_KINDS = listOf(
    AttributedTypeDef.link,
    AttributedTypeDef.email,
    AttributedTypeDef.phone,
)

/**
 * A room member's presentation, as carried by an `m.room.member` state event
 * (per-room: the same user can have a different display name / avatar in each
 * room). Used to fill a comment's sender ([MatrixMapper.comment]).
 */
class MatrixMemberInfo(
    val displayName: String?,
    val avatarUrl: String?,
)

object MatrixMapper {

    /**
     * Extract `userId -> member info` from a list of `m.room.member` state
     * events (e.g. the `state` block of a lazy-loaded `/messages` or `/sync`
     * response). The winning event per user is the last one in the list.
     * Members that left/were banned are still returned — their name is what a
     * historical message should display.
     */
    fun memberInfoMap(state: List<RoomEvent>): Map<String, MatrixMemberInfo> {
        return state
            .filter { it.type == "m.room.member" }
            .mapNotNull { event ->
                val userId = event.stateKey ?: return@mapNotNull null
                val displayName = (event.content["displayname"] as? String)
                    ?.trim()?.takeIf { it.isNotEmpty() }
                val avatarUrl = (event.content["avatar_url"] as? String)
                    ?.takeIf { it.isNotEmpty() }
                userId to MatrixMemberInfo(displayName, avatarUrl)
            }
            // Last event per user wins (toMap keeps the last on duplicate keys).
            .toMap()
    }

    fun user(
        userId: String,
        profile: ProfileGetProfileResponse?,
        service: Service,
    ): MatrixUser {
        return MatrixUser(service).apply {
            this.userId = userId
            id = ID(userId)
            name = profile?.displayname ?: userId
            this.displayName = profile?.displayname
            avatarUrl = profile?.avatarUrl
            profile?.avatarUrl?.let { iconImageUrl = it }
        }
    }

    fun user(
        userId: String,
        displayName: String?,
        avatarUrl: String?,
        service: Service,
    ): MatrixUser {
        return MatrixUser(service).apply {
            this.userId = userId
            id = ID(userId)
            name = displayName ?: userId
            this.displayName = displayName
            this.avatarUrl = avatarUrl
            if (!avatarUrl.isNullOrEmpty()) {
                iconImageUrl = avatarUrl
            }
        }
    }

    fun comment(
        event: RoomEvent,
        service: Service,
        userMe: User?,
        members: Map<String, MatrixMemberInfo>? = null,
    ): MatrixComment? {
        if (event.type != "m.room.message") return null
        if (event.content["m.encrypted"] != null) return null

        val body = event.content["body"] as? String
        val msgtype = event.content["msgtype"] as? String ?: "m.text"
        val url = event.content["url"] as? String

        return MatrixComment(service).apply {
            eventId = event.eventId
            roomId = event.roomId
            id = ID(event.eventId)
            createAt = Instant.fromEpochMilliseconds(event.originServerTs)
            this.msgtype = msgtype
            text = body?.let { AttributedString.plain(it, MATRIX_KINDS) }

            // Sender: reuse the cached self user when the event is our own,
            // otherwise resolve the display name / avatar from the room member
            // map (lazy-loaded state). Falls back to the bare user id.
            val senderId = event.sender
            user = if (senderId == (userMe as? MatrixUser)?.userId && userMe != null) {
                userMe
            } else {
                val info = members?.get(senderId)
                user(senderId, info?.displayName, info?.avatarUrl, service)
            }

            if (url != null && (msgtype == "m.image" || msgtype == "m.file" || msgtype == "m.video")) {
                medias = listOf(
                    Media().apply {
                        sourceUrl = url
                        previewUrl = url
                        type = when (msgtype) {
                            "m.image" -> MediaType.Image
                            "m.video" -> MediaType.Movie
                            else -> MediaType.File
                        }
                    }
                )
            }
        }
    }

    fun timeLine(
        events: List<RoomEvent>,
        service: Service,
        paging: Paging?,
        userMe: User?,
        members: Map<String, MatrixMemberInfo>? = null,
    ): Pageable<Comment> {
        val model = Pageable<Comment>()
        model.entities = events.mapNotNull { event ->
            comment(event, service, userMe, members)
        }.sortedByDescending { it.createAt }
        model.paging = MatrixPaging.fromPaging(paging)
        return model
    }

    fun channel(
        roomId: String,
        roomName: String?,
        service: Service,
    ): Channel {
        return Channel(service).apply {
            id = ID(roomId)
            name = roomName ?: roomId
        }
    }

    fun channel(
        summary: MatrixRoomSummary,
        service: Service,
    ): Channel {
        return Channel(service).apply {
            id = ID(summary.roomId)
            name = summary.displayName
            description = summary.topic
            summary.createAtMs?.let { createAt = Instant.fromEpochMilliseconds(it) }
        }
    }

    fun channels(
        summaries: List<MatrixRoomSummary>,
        service: Service,
        paging: Paging?,
    ): Pageable<Channel> {
        val model = Pageable<Channel>()
        model.entities = summaries.map { channel(it, service) }
        model.paging = pagingWithCount(paging, model.entities.size)
        return model
    }

    fun space(
        summary: MatrixRoomSummary,
        service: Service,
    ): MatrixSpace {
        return MatrixSpace(service).apply {
            id = ID(summary.roomId)
            roomId = summary.roomId
            name = summary.displayName
            description = summary.topic
            iconUrl = summary.avatarUrl
            memberCount = summary.memberCount
            summary.createAtMs?.let { createAt = Instant.fromEpochMilliseconds(it) }
        }
    }

    fun spaces(
        summaries: List<MatrixRoomSummary>,
        service: Service,
        paging: Paging?,
    ): Pageable<Space> {
        val model = Pageable<Space>()
        model.entities = summaries.map { space(it, service) }
        model.paging = pagingWithCount(paging, model.entities.size)
        return model
    }

    fun threads(
        summaries: List<MatrixRoomSummary>,
        service: Service,
        paging: Paging?,
    ): Pageable<Thread> {
        val model = Pageable<Thread>()
        model.entities = summaries.map { summary ->
            Thread(service).apply {
                id = ID(summary.roomId)
                description = summary.displayName
            }
        }
        model.paging = pagingWithCount(paging, model.entities.size)
        return model
    }

    /**
     * Build a [MatrixPaging] with a non-null [Paging.count]. The `Pageable`
     * paging setter runs [Paging.setMarkPagingEnd], which dereferences `count!!`
     * — an empty list with a null count would otherwise throw.
     */
    private fun pagingWithCount(paging: Paging?, size: Int): MatrixPaging {
        return MatrixPaging.fromPaging(paging).also {
            it.count = it.count ?: size.coerceAtLeast(1)
        }
    }

    /**
     * `RoomEventFilter` for `/messages`. `lazy_load_members=true` makes the
     * server include the `m.room.member` state events of the timeline's senders
     * in the response `state` block, so the sender display name / avatar can be
     * resolved without a per-sender profile lookup (no N+1).
     */
    const val MESSAGES_FILTER_JSON: String =
        "{\"lazy_load_members\":true}"

    fun createGetMessagesRequest(
        roomId: String,
        paging: MatrixPaging,
        count: Int,
    ): RoomsGetMessagesRequest {
        return RoomsGetMessagesRequest().apply {
            this.roomId = roomId
            from = paging.from
            to = paging.to
            dir = paging.direction ?: "b"
            limit = count
            filter = MESSAGES_FILTER_JSON
        }
    }

    fun usersToPageable(
        members: RoomsGetJoinedMembersResponse,
        service: Service,
        paging: Paging?,
    ): Pageable<User> {
        val model = Pageable<User>()
        model.entities = members.joined.map { (userId, member) ->
            user(userId, member.displayName, member.avatarUrl, service)
        }
        model.paging = MatrixPaging.fromPaging(paging)
        return model
    }

    fun notification(
        notification: NotificationsGetResponse.Notification,
        service: Service,
    ): Notification {
        return Notification(service).apply {
            id = ID(notification.event.eventId)
            createAt = Instant.fromEpochMilliseconds(notification.ts)
            type = notification.event.type
            users = listOf(User(service).apply {
                id = ID(notification.event.sender)
                name = notification.event.sender
            })
        }
    }

    fun notifications(
        notifications: Array<NotificationsGetResponse.Notification>,
        service: Service,
        paging: Paging?,
    ): Pageable<Notification> {
        return Pageable<Notification>().also { p ->
            p.entities = notifications.map { notification(it, service) }
            p.paging = MatrixPaging.fromPaging(paging)
        }
    }
}
