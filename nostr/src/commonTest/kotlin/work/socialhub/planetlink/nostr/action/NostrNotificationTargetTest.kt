package work.socialhub.planetlink.nostr.action

import work.socialhub.knostr.EventKind
import work.socialhub.knostr.entity.NostrEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NostrNotificationTargetTest {

    @Test
    fun mentionTargetsTheNotificationEventItself() {
        val event = event(
            id = "mention",
            kind = EventKind.TEXT_NOTE,
            tags = listOf(listOf("e", "thread-root")),
        )

        assertEquals("mention", NostrAction.targetEventId(event))
    }

    @Test
    fun repostTargetsTheRepostedEvent() {
        val event = event(
            id = "repost",
            kind = EventKind.REPOST,
            tags = listOf(listOf("e", "original"), listOf("p", "author")),
        )

        assertEquals("original", NostrAction.targetEventId(event))
    }

    @Test
    fun reactionTargetsTheLastEventTag() {
        val event = event(
            id = "reaction",
            kind = EventKind.REACTION,
            tags = listOf(listOf("e", "thread-root"), listOf("e", "reacted")),
        )

        assertEquals("reacted", NostrAction.targetEventId(event))
    }

    @Test
    fun zapReceiptTargetsTheZappedEvent() {
        val event = event(
            id = "zap",
            kind = EventKind.ZAP_RECEIPT,
            tags = listOf(listOf("e", "zapped"), listOf("p", "recipient")),
        )

        assertEquals("zapped", NostrAction.targetEventId(event))
    }

    @Test
    fun zapReceiptFallsBackToTheZapRequest() {
        val request = """
            {"kind":9734,"pubkey":"sender","tags":[["p","recipient"],["e","zapped"]]}
        """.trimIndent()
        val event = event(
            id = "zap",
            kind = EventKind.ZAP_RECEIPT,
            tags = listOf(listOf("description", request), listOf("p", "recipient")),
        )

        assertEquals("zapped", NostrAction.targetEventId(event))
    }

    @Test
    fun zapReceiptWithoutReferenceHasNoTarget() {
        val event = event(
            id = "zap",
            kind = EventKind.ZAP_RECEIPT,
            tags = listOf(listOf("description", "not a json"), listOf("p", "recipient")),
        )

        assertNull(NostrAction.targetEventId(event))
    }

    @Test
    fun reactionWithoutReferenceHasNoTarget() {
        val event = event(
            id = "reaction",
            kind = EventKind.REACTION,
            tags = listOf(listOf("p", "author")),
        )

        assertNull(NostrAction.targetEventId(event))
    }

    private fun event(
        id: String,
        kind: Int,
        tags: List<List<String>>,
    ): NostrEvent {
        return NostrEvent(
            id = id,
            pubkey = "sender",
            createdAt = 0,
            kind = kind,
            tags = tags,
            content = "",
            sig = "",
        )
    }
}
