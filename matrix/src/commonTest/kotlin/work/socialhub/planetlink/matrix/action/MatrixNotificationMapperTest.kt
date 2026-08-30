package work.socialhub.planetlink.matrix.action

import kotlinx.serialization.json.JsonPrimitive
import work.socialhub.kmatrix.api.response.notifications.NotificationsGetResponse
import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.matrix.model.MatrixUser
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Offline tests for the notification mapping: a notified `m.room.message` must
 * carry the message itself as its target comment and the sender as a resolved
 * user, so a client can show which post the notification is about. Pure mapper —
 * no network.
 */
class MatrixNotificationMapperTest {

    private val sender = "@sender:example.org"
    private val roomId = "!room:example.org"

    @Test
    fun messageNotificationCarriesTargetAndSender() {
        val service = service()
        val notification = notification("m.room.message", "\$event")
        val target = MatrixMapper.comment(
            message("\$event", "Hello there"),
            service,
            null,
        )!!

        val mapped = MatrixMapper.notification(
            notification,
            service,
            targets = mapOf("\$event" to target as Comment),
            senders = mapOf(sender to matrixUser(service, "Sender")),
        )

        assertEquals("m.room.message", mapped.type)
        assertEquals(NotificationActionType.MENTION.code, mapped.action)
        assertEquals("Hello there", mapped.comments?.first()?.text?.displayText)
        assertEquals("Sender", mapped.users?.first()?.name)
    }

    @Test
    fun messageNotificationKeepsTheRoomIdOfItsTarget() {
        val service = service()
        val target = MatrixMapper.comment(
            message("\$event", "Hello there"),
            service,
            null,
        )!!

        val mapped = MatrixMapper.notification(
            notification("m.room.message", "\$event"),
            service,
            targets = mapOf("\$event" to target as Comment),
        )

        val comment = mapped.comments?.first()
        assertTrue(comment is work.socialhub.planetlink.matrix.model.MatrixComment)
        assertEquals(roomId, comment.roomId)
    }

    @Test
    fun unresolvedSenderFallsBackToTheUserId() {
        val service = service()

        val mapped = MatrixMapper.notification(
            notification("m.room.message", "\$event"),
            service,
        )

        assertEquals(sender, mapped.users?.first()?.name)
        assertNull(mapped.comments)
    }

    @Test
    fun nonMessageNotificationHasNoCommonAction() {
        val service = service()

        val mapped = MatrixMapper.notification(
            notification("m.room.member", "\$invite"),
            service,
        )

        assertEquals("m.room.member", mapped.type)
        assertNull(mapped.action)
        assertNull(mapped.comments)
    }

    private fun service(): Service {
        val account = Account()
        return Service("matrix", account)
    }

    private fun matrixUser(service: Service, displayName: String): User {
        return MatrixUser(service).apply {
            userId = sender
            name = displayName
        }
    }

    private fun message(
        eventId: String,
        body: String,
    ): RoomEvent = RoomEvent().apply {
        type = "m.room.message"
        this.eventId = eventId
        sender = this@MatrixNotificationMapperTest.sender
        roomId = this@MatrixNotificationMapperTest.roomId
        originServerTs = 1_000
        content = mapOf("msgtype" to "m.text", "body" to body)
    }

    private fun notification(
        type: String,
        eventId: String,
    ): NotificationsGetResponse.Notification {
        return NotificationsGetResponse.Notification().apply {
            this.roomId = this@MatrixNotificationMapperTest.roomId
            ts = 1_000
            event = NotificationsGetResponse.Event().apply {
                this.type = type
                this.eventId = eventId
                sender = this@MatrixNotificationMapperTest.sender
                originServerTs = 1_000
                content = mapOf(
                    "msgtype" to JsonPrimitive("m.text"),
                    "body" to JsonPrimitive("Hello there"),
                )
            }
        }
    }
}
