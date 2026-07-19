package work.socialhub.planetlink.matrix.action

import work.socialhub.kmatrix.api.response.rooms.RoomEvent
import work.socialhub.kmatrix.api.response.sync.SyncResponse
import work.socialhub.kmatrix.api.response.sync.SyncRoomSummary

/**
 * A parsed, per-room view of a single `/sync` response. Building this once lets
 * `spaces()` / `channels()` / `messageThread()` serve their lists without any
 * per-room follow-up call (no N+1). See [MatrixSnapshotParser].
 */
class MatrixRoomSummary(
    val roomId: String,
    /** Display name computed per the Matrix room-naming algorithm. */
    val displayName: String,
    val topic: String?,
    /** `mxc://` avatar url, if any. */
    val avatarUrl: String?,
    /** Room creation time (from `m.room.create`) in epoch millis. */
    val createAtMs: Long?,
    /** `m.room.create` content declares `type: "m.space"`. */
    val isSpace: Boolean,
    /** Direct message room (from `m.direct`, or a 2-member fallback). */
    val isDirect: Boolean,
    /** Direct child room ids (`m.space.child`), ordered per MSC1772. */
    val childRoomIds: List<String>,
    /** Joined + invited member count. */
    val memberCount: Int,
)

/** All joined rooms of a sync, keyed by room id, plus the raw DM room-id set. */
class MatrixSnapshot(
    val rooms: Map<String, MatrixRoomSummary>,
    val directRoomIds: Set<String>,
)

/**
 * Pure (non-suspend) parsing of a [SyncResponse] into a [MatrixSnapshot].
 * Kept free of any network access so it is fully unit-testable and unaffected
 * by the Kotlin/JS yield* bridge issue.
 */
object MatrixSnapshotParser {

    fun parse(sync: SyncResponse, selfUserId: String): MatrixSnapshot {
        val directRoomIds = directRoomIds(sync)
        val joined = sync.rooms?.join ?: emptyMap()

        val rooms = joined.mapValues { (roomId, room) ->
            summarize(
                roomId = roomId,
                state = room.state?.events?.toList() ?: emptyList(),
                summary = room.summary,
                directRoomIds = directRoomIds,
                selfUserId = selfUserId,
            )
        }
        return MatrixSnapshot(rooms, directRoomIds)
    }

    private fun summarize(
        roomId: String,
        state: List<RoomEvent>,
        summary: SyncRoomSummary?,
        directRoomIds: Set<String>,
        selfUserId: String,
    ): MatrixRoomSummary {
        val create = lastStateEvent(state, "m.room.create")
        val isSpace = (create?.content?.get("type") as? String) == "m.space"
        val createAtMs = create?.originServerTs

        val topic = (lastStateEvent(state, "m.room.topic")?.content?.get("topic") as? String)
            ?.trim()?.takeIf { it.isNotEmpty() }
        val avatarUrl = (lastStateEvent(state, "m.room.avatar")?.content?.get("url") as? String)
            ?.takeIf { it.isNotEmpty() }

        // Prefer the server-computed summary (works with lazy_load_members),
        // falling back to counting the member state events when it is absent.
        val heroes = summary?.heroes?.toList()
        val members = latestPerStateKey(state, "m.room.member")
        val memberCount = summary?.joinedMemberCount
            ?: members.count { membership(it) == "join" || membership(it) == "invite" }

        val childRoomIds = latestPerStateKey(state, "m.space.child")
            // An empty content (or one lacking `via`) marks a removed child.
            .filter { it.content.isNotEmpty() && it.content["via"] != null }
            .sortedWith(
                compareBy(
                    { (it.content["order"] as? String) ?: "￿" },
                    { it.originServerTs },
                    { it.stateKey ?: "" },
                )
            )
            .mapNotNull { it.stateKey }

        return MatrixRoomSummary(
            roomId = roomId,
            displayName = roomDisplayName(state, selfUserId, heroes, memberCount),
            topic = topic,
            avatarUrl = avatarUrl,
            createAtMs = createAtMs,
            isSpace = isSpace,
            isDirect = (roomId in directRoomIds) || (memberCount == 2),
            childRoomIds = childRoomIds,
            memberCount = memberCount,
        )
    }

    /**
     * Compute a room's display name following the Matrix Client-Server spec
     * ("Calculating the display name for a room"):
     * `m.room.name` → `m.room.canonical_alias` → members/heroes fallback → "Empty room".
     *
     * Ported from matrix-js-sdk `Room.calculateRoomName` + `memberNamesToRoomName`
     * (element-web's naming). [heroes] (from the sync room `summary`) takes
     * precedence over the member-event scan when present; otherwise the fallback
     * is derived from the `m.room.member` state events, excluding the current
     * user. [memberCount] is the joined+invited count (self included), used to
     * form the "and N others" suffix the same way element does.
     */
    fun roomDisplayName(
        state: List<RoomEvent>,
        selfUserId: String,
        heroes: List<String>? = null,
        memberCount: Int = 0,
    ): String {
        (lastStateEvent(state, "m.room.name")?.content?.get("name") as? String)
            ?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        (lastStateEvent(state, "m.room.canonical_alias")?.content?.get("alias") as? String)
            ?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val members = latestPerStateKey(state, "m.room.member")
        val nameOf: (String) -> String = { uid ->
            val member = members.firstOrNull { (it.stateKey ?: "") == uid }
            (member?.content?.get("displayname") as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: uid
        }

        // element counts other members as joined+invited-1 (excluding self), so
        // "and N others" is accurate even when we only hold a few hero names.
        val otherCount = (memberCount - 1).coerceAtLeast(0)
        val otherNames: List<String> = if (!heroes.isNullOrEmpty()) {
            heroes.filter { it != selfUserId }.map(nameOf)
        } else {
            members
                .filter { membership(it) == "join" || membership(it) == "invite" }
                .mapNotNull { it.stateKey }
                .filter { it != selfUserId }
                .distinct()
                .sorted()
                .take(5) // imitate summary heroes
                .map(nameOf)
        }

        return memberNamesToRoomName(otherNames, otherCount)
    }

    /**
     * Port of matrix-js-sdk `memberNamesToRoomName` ([count] is the number of
     * other members, excluding self). The "and N others" suffix counts the
     * members beyond the first named one (`count - 1`), so a room of 3 with a
     * single known name renders "Alice and 1 other", not "Alice and 2 others".
     */
    private fun memberNamesToRoomName(names: List<String>, count: Int): String {
        val countWithoutMe = count - 1
        return when {
            names.isEmpty() -> "Empty room"
            names.size == 1 && countWithoutMe <= 1 -> names[0]
            names.size == 2 && countWithoutMe <= 2 -> "${names[0]} and ${names[1]}"
            countWithoutMe > 1 -> "${names[0]} and $countWithoutMe others"
            else -> "${names[0]} and 1 other"
        }
    }

    /** Room ids referenced by the top-level `m.direct` account-data event. */
    private fun directRoomIds(sync: SyncResponse): Set<String> {
        return sync.accountData?.events
            ?.lastOrNull { it.type == "m.direct" }
            ?.content?.values
            ?.flatMap { (it as? List<*>).orEmpty() }
            ?.filterIsInstance<String>()
            ?.toSet()
            ?: emptySet()
    }

    private fun membership(event: RoomEvent): String? =
        event.content["membership"] as? String

    /** The last (winning) state event for [type] with an empty state key. */
    private fun lastStateEvent(state: List<RoomEvent>, type: String): RoomEvent? =
        state.lastOrNull { it.type == type && (it.stateKey ?: "").isEmpty() }

    /** The winning (last) state event per state key for [type]. */
    private fun latestPerStateKey(state: List<RoomEvent>, type: String): List<RoomEvent> =
        state.filter { it.type == type }
            .groupBy { it.stateKey ?: "" }
            .map { (_, events) -> events.last() }
}
