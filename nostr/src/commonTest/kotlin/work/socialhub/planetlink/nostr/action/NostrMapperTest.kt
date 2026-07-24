package work.socialhub.planetlink.nostr.action

import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.util.Bech32
import work.socialhub.knostr.util.Hex
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NostrMapperTest {

    @Test
    fun removesResolvedQuoteReferenceFromDisplayText() {
        val quotedEventId = "22".repeat(32)
        val quotedNote = note(
            eventId = quotedEventId,
            content = "Quoted content",
        )
        val reference = nevent(quotedEventId)
        val source = note(
            eventId = "11".repeat(32),
            content = "Additional comment\n\nnostr:$reference",
        ).apply {
            this.quotedEventId = quotedEventId
            this.quotedNote = quotedNote
        }

        val comment = NostrMapper.comment(source, service())

        assertEquals("Additional comment", comment.text?.displayText)
        assertNotNull(comment.sharedComment)
    }

    @Test
    fun preservesReferenceToAnotherEvent() {
        val quotedEventId = "22".repeat(32)
        val otherEventId = "33".repeat(32)
        val otherReference = nevent(otherEventId)
        val source = note(
            eventId = "11".repeat(32),
            content = "See also nostr:$otherReference",
        ).apply {
            this.quotedEventId = quotedEventId
            this.quotedNote = note(quotedEventId, "Quoted content")
        }

        val comment = NostrMapper.comment(source, service())

        assertEquals("See also nostr:$otherReference", comment.text?.displayText)
    }

    @Test
    fun preservesQuoteReferenceUntilQuoteIsResolved() {
        val quotedEventId = "22".repeat(32)
        val reference = nevent(quotedEventId)
        val source = note(
            eventId = "11".repeat(32),
            content = "Additional comment\n\nnostr:$reference",
        ).apply {
            this.quotedEventId = quotedEventId
        }

        val comment = NostrMapper.comment(source, service())

        assertEquals(
            "Additional comment\n\nnostr:$reference",
            comment.text?.displayText,
        )
    }

    private fun note(
        eventId: String,
        content: String,
    ): NostrNote {
        return NostrNote().apply {
            event = NostrEvent(
                id = eventId,
                pubkey = "44".repeat(32),
                createdAt = 1_000,
                kind = 1,
                tags = emptyList(),
                content = content,
                sig = "55".repeat(64),
            )
            this.content = content
            createdAt = event.createdAt
            noteId = Bech32.encode("note", Hex.decode(eventId))
        }
    }

    private fun nevent(eventId: String): String {
        val tlv = byteArrayOf(0, 32) + Hex.decode(eventId)
        return Bech32.encode("nevent", tlv)
    }

    private fun service(): Service {
        val account = Account()
        return Service("nostr", account).also {
            account.service = it
        }
    }
}
