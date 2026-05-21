package work.socialhub.planetlink.matrix.action

import kotlin.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import work.socialhub.kmatrix.api.request.events.EventsGetContextRequest
import work.socialhub.kmatrix.api.request.notifications.NotificationsGetRequest
import work.socialhub.kmatrix.api.request.rooms.RoomsGetMessagesRequest
import work.socialhub.kmatrix.api.request.rooms.RoomsRedactEventRequest
import work.socialhub.kmatrix.api.request.rooms.RoomsSendMessageRequest
import work.socialhub.kmatrix.api.request.userdirectory.UserDirectorySearchRequest
import work.socialhub.kmatrix.api.response.events.EventsGetContextResponse
import work.socialhub.kmatrix.api.response.events.EventsGetEventResponse
import work.socialhub.kmatrix.api.response.notifications.NotificationsGetResponse
import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.kmatrix.stream.MatrixStreamFactory
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.matrix.model.MatrixComment
import work.socialhub.planetlink.matrix.model.MatrixPaging
import work.socialhub.planetlink.matrix.model.MatrixUser
import kotlin.js.JsExport

@JsExport
class MatrixAction(
    account: Account,
    val auth: MatrixAuth,
) : AccountActionImpl(account) {

    private val accessor get() = auth.accessor

    override suspend fun userMe(): User {
        return proceed {
            val whoami = accessor.accounts().whoami().data
            val profile = accessor.profile().getProfile(whoami.userId).data
            val user = MatrixMapper.user(whoami.userId, profile, service())
            me = user
            user
        }
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
        reactionComment(id, "\uD83D\uDC4D")
    }

    override suspend fun unlikeComment(id: Identify) {
        unreactionComment(id, "\uD83D\uDC4D")
    }

    override suspend fun shareComment(id: Identify) {
        throw NotSupportedException("Matrix does not support sharing comments")
    }

    override suspend fun unshareComment(id: Identify) {
        throw NotSupportedException("Matrix does not support unsharing comments")
    }

    override suspend fun reactionComment(id: Identify, reaction: String) {
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
        proceedUnit {
            val comment = id as? MatrixComment
                ?: throw SocialHubException("Not a Matrix comment")

            throw NotSupportedException("Need reaction event lookup before unreaction redact")
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

    override suspend fun channels(id: Identify, paging: Paging): Pageable<Channel> {
        return proceed {
            val rooms = accessor.rooms().getJoinedRooms().data.joinedRooms
            val channels = rooms.map { roomId ->
                val name = try {
                    accessor.rooms().getRoomName(roomId).data.name
                } catch (_: Exception) { null }
                MatrixMapper.channel(roomId, name, service())
            }
            Pageable<Channel>().also {
                it.entities = channels
                it.paging = paging
            }
        }
    }

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return proceed {
            val roomId = id.id!!.value<String>()
            val mp = MatrixPaging.fromPaging(paging)
            val response = accessor.rooms().getMessages(
                MatrixMapper.createGetMessagesRequest(roomId, mp, paging.count ?: 50)
            ).data

            val userMe = userMeWithCache()
            val pageable = MatrixMapper.timeLine(
                response.chunk.toList(),
                service(),
                paging,
                userMe,
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
        return proceed {
            val rooms = accessor.rooms().getJoinedRooms().data.joinedRooms
            val userMeId = (userMeWithCache() as? MatrixUser)?.userId ?: ""

            val dmRooms = rooms.filter { roomId ->
                try {
                    val members = accessor.rooms().getJoinedMembers(roomId).data.joined
                    members.size == 2 && members.keys.any { it != userMeId }
                } catch (_: Exception) { false }
            }

            val threads = dmRooms.map { roomId ->
                val roomName = try {
                    accessor.rooms().getRoomName(roomId).data.name
                } catch (_: Exception) { null }
                Thread(service()).apply {
                    id = ID(roomId)
                    description = roomName
                }
            }

            Pageable<Thread>().also {
                it.entities = threads
                it.paging = paging
            }
        }
    }

    override suspend fun messageTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return channelTimeLine(id, paging)
    }

    override suspend fun postMessage(req: CommentForm) {
        postComment(req)
    }

    override suspend fun setHomeTimeLineStream(callback: EventCallback): Stream {
        val kmatrixStream = MatrixStreamFactory.instance(
            auth.host,
            auth.accessToken ?: ""
        )
        val stream = MatrixStream(kmatrixStream, callback)
        stream.open()
        return stream
    }

    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        return setHomeTimeLineStream(callback)
    }

    override fun request(): RequestAction {
        return MatrixRequest(account)
    }

    private fun service(): Service = account.service

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return try {
            runner()
        } catch (e: SocialHubException) { throw e }
        catch (e: Exception) { throw SocialHubException(e.message, e) }
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        try {
            runner()
        } catch (e: SocialHubException) { throw e }
        catch (e: Exception) { throw SocialHubException(e.message, e) }
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

private fun extractJsonValue(element: JsonElement): Any? {
    return when {
        element.jsonPrimitive.isString -> element.jsonPrimitive.content
        element.jsonPrimitive.contentOrNull != null -> element.jsonPrimitive.content
        else -> null
    }
}
