package work.socialhub.planetlink.matrix.action

import kotlinx.datetime.Instant
import work.socialhub.kmatrix.api.request.rooms.RoomsGetMessagesRequest
import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.kmatrix.api.response.rooms.RoomsGetJoinedMembersResponse
import work.socialhub.kmatrix.api.response.profile.ProfileGetProfileResponse
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.matrix.model.MatrixComment
import work.socialhub.planetlink.matrix.model.MatrixPaging
import work.socialhub.planetlink.matrix.model.MatrixUser

object MatrixMapper {

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
            text = body?.let { AttributedString.plain(it) }

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
    ): Pageable<Comment> {
        val model = Pageable<Comment>()
        model.entities = events.mapNotNull { event ->
            comment(event, service, userMe)
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
}
