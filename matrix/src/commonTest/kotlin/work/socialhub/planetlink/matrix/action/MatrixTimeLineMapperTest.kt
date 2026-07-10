package work.socialhub.planetlink.matrix.action

import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.planetlink.matrix.model.MatrixUser
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Offline tests for the sender-user resolution added to the channel timeline:
 * each `m.room.message` must carry a [MatrixUser] built from `event.sender`,
 * enriched with the display name / avatar from the room's lazy-loaded
 * `m.room.member` state. Pure mapper — no network.
 */
class MatrixTimeLineMapperTest {

    private val self = "@me:example.org"

    private fun service(): Service {
        val account = Account()
        return Service("matrix", account)
    }

    private fun message(
        eventId: String,
        sender: String,
        body: String,
    ): RoomEvent = RoomEvent().apply {
        type = "m.room.message"
        this.eventId = eventId
        this.sender = sender
        roomId = "!r:example.org"
        originServerTs = 1_000
        content = mapOf("msgtype" to "m.text", "body" to body)
    }

    private fun member(
        userId: String,
        displayName: String?,
        avatarUrl: String?,
    ): RoomEvent = RoomEvent().apply {
        type = "m.room.member"
        stateKey = userId
        sender = userId
        content = buildMap {
            put("membership", "join")
            if (displayName != null) put("displayname", displayName)
            if (avatarUrl != null) put("avatar_url", avatarUrl)
        }
    }

    @Test
    fun testSenderResolvedFromMemberState() {
        val members = MatrixMapper.memberInfoMap(
            listOf(member("@alice:example.org", "Alice", "mxc://example.org/a")),
        )
        val pageable = MatrixMapper.timeLine(
            listOf(message("\$1", "@alice:example.org", "hi")),
            service(),
            Paging(),
            userMe = null,
            members = members,
        )

        val user = pageable.entities.first().user as MatrixUser
        assertEquals("@alice:example.org", user.userId)
        assertEquals("Alice", user.name)
        assertEquals("Alice", user.displayName)
        assertEquals("mxc://example.org/a", user.avatarUrl)
    }

    @Test
    fun testSenderFallsBackToUserIdWhenNoMember() {
        // No member state for the sender (e.g. it wasn't in the response's
        // lazy-loaded state): the bare user id must still be exposed.
        val pageable = MatrixMapper.timeLine(
            listOf(message("\$1", "@bob:example.org", "hey")),
            service(),
            Paging(),
            userMe = null,
            members = emptyMap(),
        )

        val user = pageable.entities.first().user as MatrixUser
        assertNotNull(user)
        assertEquals("@bob:example.org", user.userId)
        // name falls back to the user id, never null.
        assertEquals("@bob:example.org", user.name)
        assertNull(user.displayName)
    }

    @Test
    fun testOwnMessageReusesSelfUser() {
        val me = MatrixUser(service()).apply {
            userId = self
            name = "Me"
            displayName = "Me"
        }
        val pageable = MatrixMapper.timeLine(
            listOf(message("\$1", self, "mine")),
            service(),
            Paging(),
            userMe = me,
            members = emptyMap(),
        )

        // The self user object is reused verbatim (identity), not rebuilt.
        assertEquals(me, pageable.entities.first().user)
    }

    @Test
    fun testMemberInfoMapLastEventWins() {
        val members = MatrixMapper.memberInfoMap(
            listOf(
                member("@alice:example.org", "Alice", null),
                member("@alice:example.org", "Alice (new)", "mxc://example.org/new"),
            ),
        )
        val info = members["@alice:example.org"]!!
        assertEquals("Alice (new)", info.displayName)
        assertEquals("mxc://example.org/new", info.avatarUrl)
    }
}
