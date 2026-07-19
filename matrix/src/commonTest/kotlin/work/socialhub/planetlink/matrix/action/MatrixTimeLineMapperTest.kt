package work.socialhub.planetlink.matrix.action

import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.kmatrix.api.response.relations.RelationsGetResponse
import work.socialhub.planetlink.matrix.model.MatrixUser
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

    private fun reaction(
        eventId: String,
        sender: String,
        targetEventId: String,
        key: String,
    ): RoomEvent = RoomEvent().apply {
        type = "m.reaction"
        this.eventId = eventId
        this.sender = sender
        roomId = "!r:example.org"
        originServerTs = 2_000
        content = mapOf(
            "m.relates_to" to mapOf(
                "rel_type" to "m.annotation",
                "event_id" to targetEventId,
                "key" to key,
            )
        )
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

    @Test
    fun testTimelineCollectsReactionEvents() {
        val me = MatrixUser(service()).apply { userId = self }
        val pageable = MatrixMapper.timeLine(
            listOf(
                message("\$message", "@alice:example.org", "hi"),
                reaction("\$reaction-1", self, "\$message", "\uD83D\uDC4D"),
                reaction("\$reaction-2", "@bob:example.org", "\$message", "\uD83D\uDC4D"),
            ),
            service(),
            Paging(),
            userMe = me,
        )

        val reaction = pageable.entities.single().reactions.single()
        assertEquals("\uD83D\uDC4D", reaction.name)
        assertEquals("\uD83D\uDC4D", reaction.emoji)
        assertEquals(2, reaction.count)
        assertEquals(true, reaction.reacting)
    }

    @Test
    fun testBundledReactionCountIsNotDoubleCounted() {
        val event = message("\$message", "@alice:example.org", "hi").apply {
            unsigned = mapOf(
                "m.relations" to mapOf(
                    "m.annotation" to mapOf(
                        "chunk" to listOf(
                            mapOf("type" to "m.reaction", "key" to "\uD83D\uDC4D", "count" to 5)
                        )
                    )
                )
            )
        }
        val me = MatrixUser(service()).apply { userId = self }
        val pageable = MatrixMapper.timeLine(
            listOf(event, reaction("\$mine", self, "\$message", "\uD83D\uDC4D")),
            service(),
            Paging(),
            userMe = me,
        )

        val reaction = pageable.entities.single().reactions.single()
        assertEquals(5, reaction.count)
        assertEquals(true, reaction.reacting)
    }

    @Test
    fun testRelationsResponseMapsReactionCounts() {
        fun relation(sender: String, key: String) =
            RelationsGetResponse.RelationEvent().apply {
                type = "m.reaction"
                this.sender = sender
                content = mapOf(
                    "m.relates_to" to buildJsonObject {
                        put("rel_type", "m.annotation")
                        put("event_id", "\$message")
                        put("key", key)
                    }
                )
            }

        val reactions = MatrixMapper.reactions(
            listOf(
                relation(self, "\uD83C\uDF89"),
                relation("@alice:example.org", "\uD83C\uDF89"),
                relation("@bob:example.org", "\uD83D\uDC4D"),
            ),
            self,
        )

        assertEquals(2, reactions.size)
        assertEquals(2, reactions.first { it.name == "\uD83C\uDF89" }.count)
        assertEquals(true, reactions.first { it.name == "\uD83C\uDF89" }.reacting)
        assertEquals(1, reactions.first { it.name == "\uD83D\uDC4D" }.count)
        assertEquals(false, reactions.first { it.name == "\uD83D\uDC4D" }.reacting)
    }
}
