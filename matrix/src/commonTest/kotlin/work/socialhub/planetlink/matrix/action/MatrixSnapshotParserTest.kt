package work.socialhub.planetlink.matrix.action

import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.kmatrix.api.response.sync.SyncAccountData
import work.socialhub.kmatrix.api.response.sync.SyncJoinedRoom
import work.socialhub.kmatrix.api.response.sync.SyncResponse
import work.socialhub.kmatrix.api.response.sync.SyncRoomSummary
import work.socialhub.kmatrix.api.response.sync.SyncRooms
import work.socialhub.kmatrix.api.response.sync.SyncState
import work.socialhub.planetlink.matrix.model.MatrixSpace
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Offline tests for the single-sync parsing that backs spaces() / channels() /
 * messageThread(). Everything here is fed a hand-built [SyncResponse] — no
 * network — which is the structural proof that the list building no longer
 * issues per-room getRoomName / getJoinedMembers calls (the old N+1).
 */
class MatrixSnapshotParserTest {

    private val self = "@me:example.org"

    private fun service(): Service {
        val account = Account()
        return Service("matrix", account)
    }

    private fun event(
        type: String,
        content: Map<String, Any?>,
        stateKey: String? = "",
        ts: Long = 0,
    ): RoomEvent = RoomEvent().apply {
        this.type = type
        this.content = content
        this.stateKey = stateKey
        this.originServerTs = ts
        this.eventId = "\$evt_${type}_${stateKey}"
        this.sender = self
    }

    private fun joinedRoom(
        vararg events: RoomEvent,
        summary: SyncRoomSummary? = null,
    ): SyncJoinedRoom =
        SyncJoinedRoom().apply {
            state = SyncState().apply { this.events = arrayOf(*events) }
            this.summary = summary
        }

    private fun sync(
        join: Map<String, SyncJoinedRoom>,
        direct: Map<String, List<String>>? = null,
    ): SyncResponse = SyncResponse().apply {
        rooms = SyncRooms().apply { this.join = join }
        if (direct != null) {
            accountData = SyncAccountData().apply {
                events = arrayOf(event("m.direct", direct, stateKey = null))
            }
        }
    }

    private fun memberEvent(userId: String, displayName: String?, membership: String = "join") =
        event(
            "m.room.member",
            buildMap {
                put("membership", membership)
                if (displayName != null) put("displayname", displayName)
            },
            stateKey = userId,
        )

    @Test
    fun testNamePrefersRoomName() {
        val state = listOf(
            event("m.room.name", mapOf("name" to "  General  ")),
            event("m.room.canonical_alias", mapOf("alias" to "#general:example.org")),
            memberEvent("@alice:example.org", "Alice"),
        )
        assertEquals("General", MatrixSnapshotParser.roomDisplayName(state, self))
    }

    @Test
    fun testNameFallsBackToCanonicalAlias() {
        val state = listOf(
            event("m.room.canonical_alias", mapOf("alias" to "#room:example.org")),
            memberEvent("@alice:example.org", "Alice"),
        )
        assertEquals("#room:example.org", MatrixSnapshotParser.roomDisplayName(state, self))
    }

    @Test
    fun testNameFallsBackToTwoMembersWithAnd() {
        val state = listOf(
            memberEvent(self, "Me"),
            memberEvent("@alice:example.org", "Alice"),
            memberEvent("@bob:example.org", "Bob"),
        )
        // element-web: two others -> "A and B" (count = 3 joined incl. self).
        assertEquals("Alice and Bob", MatrixSnapshotParser.roomDisplayName(state, self, memberCount = 3))
    }

    @Test
    fun testNameForManyMembersSummarizes() {
        val state = (1..6).map { memberEvent("@u$it:example.org", "User$it") }
        // 6 joined incl. self -> one name + "and N others" (N = count - 1 = 5).
        assertEquals("User1 and 5 others", MatrixSnapshotParser.roomDisplayName(state, self, memberCount = 6))
    }

    @Test
    fun testNameForThreeMembersUsesOneOther() {
        val state = listOf(
            memberEvent(self, "Me"),
            memberEvent("@alice:example.org", "Alice"),
            memberEvent("@bob:example.org", "Bob"),
        )
        // Only one hero name known but count says 3 -> "Alice and 2 others".
        assertEquals(
            "Alice and 2 others",
            MatrixSnapshotParser.roomDisplayName(state, self, heroes = listOf("@alice:example.org"), memberCount = 3),
        )
    }

    @Test
    fun testEmptyRoomName() {
        val state = listOf(memberEvent(self, "Me"))
        assertEquals("Empty room", MatrixSnapshotParser.roomDisplayName(state, self, memberCount = 1))
    }

    @Test
    fun testHeroesTakePrecedenceOverMemberScan() {
        val state = listOf(
            memberEvent("@alice:example.org", "Alice"),
            memberEvent("@bob:example.org", "Bob"),
        )
        // Heroes list narrows the name to just Bob (count = 2 -> single other).
        assertEquals(
            "Bob",
            MatrixSnapshotParser.roomDisplayName(state, self, heroes = listOf("@bob:example.org"), memberCount = 2),
        )
    }

    @Test
    fun testParseSpaceWithOrderedChildren() {
        val spaceRoom = joinedRoom(
            event("m.room.create", mapOf("type" to "m.space")),
            event("m.room.name", mapOf("name" to "My Space")),
            event("m.space.child", mapOf("via" to listOf("example.org"), "order" to "b"), stateKey = "!child2:example.org"),
            event("m.space.child", mapOf("via" to listOf("example.org"), "order" to "a"), stateKey = "!child1:example.org"),
            // Removed child (empty content) must be ignored.
            event("m.space.child", emptyMap(), stateKey = "!removed:example.org"),
        )
        val snapshot = MatrixSnapshotParser.parse(sync(mapOf("!space:example.org" to spaceRoom)), self)
        val summary = snapshot.rooms["!space:example.org"]!!

        assertTrue(summary.isSpace)
        assertEquals("My Space", summary.displayName)
        // Ordered by `order`: a before b.
        assertEquals(listOf("!child1:example.org", "!child2:example.org"), summary.childRoomIds)
    }

    @Test
    fun testParseDirectMessageFromMDirect() {
        val dm = joinedRoom(
            event("m.room.create", emptyMap()),
            memberEvent(self, "Me"),
            memberEvent("@alice:example.org", "Alice"),
        )
        val snapshot = MatrixSnapshotParser.parse(
            sync(
                join = mapOf("!dm:example.org" to dm),
                direct = mapOf("@alice:example.org" to listOf("!dm:example.org")),
            ),
            self,
        )
        val summary = snapshot.rooms["!dm:example.org"]!!
        assertFalse(summary.isSpace)
        assertTrue(summary.isDirect)
        assertEquals("Alice", summary.displayName)
        assertEquals(2, summary.memberCount)
    }

    @Test
    fun testParseUsesServerSummaryWhenPresent() {
        // With lazy_load_members, membership events are sparse; the summary
        // (heroes + joined member count) drives the name and member count.
        val room = joinedRoom(
            memberEvent("@alice:example.org", "Alice"),
            summary = SyncRoomSummary().apply {
                heroes = arrayOf("@alice:example.org", "@bob:example.org")
                joinedMemberCount = 8
            },
        )
        val snapshot = MatrixSnapshotParser.parse(sync(mapOf("!r:example.org" to room)), self)
        val summary = snapshot.rooms["!r:example.org"]!!

        assertEquals(8, summary.memberCount)
        // 8 joined members -> element formats as "<first hero> and N others".
        assertEquals("Alice and 7 others", summary.displayName)
    }

    @Test
    fun testMappersHandleEmptySnapshotWithoutNpe() {
        val svc = service()
        // An account with zero joined rooms must not trip Paging.setMarkPagingEnd's count!!.
        val paging = Paging()
        assertEquals(0, MatrixMapper.spaces(emptyList(), svc, paging).entities.size)
        assertEquals(0, MatrixMapper.channels(emptyList(), svc, paging).entities.size)
        assertEquals(0, MatrixMapper.threads(emptyList(), svc, paging).entities.size)
    }

    @Test
    fun testSpacesMapperProducesMatrixSpace() {
        val spaceRoom = joinedRoom(
            event("m.room.create", mapOf("type" to "m.space")),
            event("m.room.name", mapOf("name" to "Team")),
            event("m.room.topic", mapOf("topic" to "Our team space")),
        )
        val snapshot = MatrixSnapshotParser.parse(sync(mapOf("!s:example.org" to spaceRoom)), self)
        val pageable = MatrixMapper.spaces(snapshot.rooms.values.filter { it.isSpace }, service(), Paging())

        assertEquals(1, pageable.entities.size)
        val space = pageable.entities.first() as MatrixSpace
        assertEquals("Team", space.name)
        assertEquals("Our team space", space.description)
        assertEquals("!s:example.org", space.roomId)
    }
}
