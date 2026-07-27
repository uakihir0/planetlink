package work.socialhub.planetlink.nostr.action

import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.util.Bech32
import work.socialhub.knostr.util.Hex
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedElement
import work.socialhub.planetlink.model.common.AttributedKind
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
    fun removesResolvedNote1QuoteReferenceFromDisplayText() {
        val quotedEventId = "22".repeat(32)
        val quotedNote = note(
            eventId = quotedEventId,
            content = "Quoted content",
        )
        val reference = note1(quotedEventId)
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
    fun removesResolvedUppercaseQuoteReferenceFromDisplayText() {
        val quotedEventId = "22".repeat(32)
        val quotedNote = note(
            eventId = quotedEventId,
            content = "Quoted content",
        )
        val reference = nevent(quotedEventId).uppercase()
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

    @Test
    fun mapsEmojiTagToAttributedElement() {
        val imageUrl = "https://example.com/emoji.webp"
        val source = note(
            eventId = "11".repeat(32),
            content = "Hello :planetlink:",
            tags = listOf(listOf("emoji", "planetlink", imageUrl)),
        )

        val emoji = emojiElements(NostrMapper.comment(source, service())).single()

        assertEquals(AttributedKind.EMOJI, emoji.kind)
        assertEquals(":planetlink:", emoji.displayText)
        assertEquals(imageUrl, emoji.expandedText)
        assertEquals(true, emoji.visible)
    }

    @Test
    fun mapsEveryOccurrenceOfSameEmoji() {
        val source = note(
            eventId = "11".repeat(32),
            content = ":wave: hello :wave:",
            tags = listOf(listOf("emoji", "wave", "https://example.com/wave.webp")),
        )

        val emojis = emojiElements(NostrMapper.comment(source, service()))

        assertEquals(2, emojis.size)
        assertEquals(listOf(":wave:", ":wave:"), emojis.map { it.displayText })
    }

    @Test
    fun mapsMultipleEmojiTypes() {
        val source = note(
            eventId = "11".repeat(32),
            content = ":wave: :planet:",
            tags = listOf(
                listOf("emoji", "wave", "https://example.com/wave.webp"),
                listOf("emoji", "planet", "https://example.com/planet.webp"),
            ),
        )

        val emojis = emojiElements(NostrMapper.comment(source, service()))

        assertEquals(listOf(":wave:", ":planet:"), emojis.map { it.displayText })
        assertEquals(
            listOf(
                "https://example.com/wave.webp",
                "https://example.com/planet.webp",
            ),
            emojis.map { it.expandedText },
        )
    }

    @Test
    fun leavesUndefinedShortcodeAsPlainText() {
        val source = note(
            eventId = "11".repeat(32),
            content = ":defined: :undefined:",
            tags = listOf(
                listOf("emoji", "defined", "https://example.com/defined.webp"),
            ),
        )

        val comment = NostrMapper.comment(source, service())

        assertEquals(":defined: :undefined:", comment.text?.displayText)
        assertEquals(listOf(":defined:"), emojiElements(comment).map { it.displayText })
        assertEquals(
            AttributedKind.PLAIN,
            comment.text?.elements?.single { it.displayText.contains(":undefined:") }?.kind,
        )
    }

    @Test
    fun ignoresMalformedAndEmptyEmojiTags() {
        val source = note(
            eventId = "11".repeat(32),
            content = ":missing: :emptyCode: :emptyUrl: :bad-shortcode: :[:",
            tags = listOf(
                listOf("emoji"),
                listOf("emoji", "missing"),
                listOf("emoji", "", "https://example.com/empty-code.webp"),
                listOf("emoji", "emptyUrl", ""),
                listOf("emoji", "   ", "https://example.com/blank-code.webp"),
                listOf("emoji", "blankUrl", "   "),
                listOf("emoji", "bad-shortcode", "https://example.com/bad.webp"),
                listOf("emoji", "[", "https://example.com/bracket.webp"),
            ),
        )

        val comment = NostrMapper.comment(source, service())

        assertEquals(emptyList(), emojiElements(comment))
        assertEquals(
            ":missing: :emptyCode: :emptyUrl: :bad-shortcode: :[:",
            comment.text?.displayText,
        )
    }

    @Test
    fun keepsFirstTagForDuplicateShortcode() {
        val source = note(
            eventId = "11".repeat(32),
            content = ":wave:",
            tags = listOf(
                listOf("emoji", "wave", "https://example.com/first.webp"),
                listOf("emoji", "wave", "https://example.com/second.webp"),
            ),
        )

        val emoji = emojiElements(NostrMapper.comment(source, service())).single()

        assertEquals("https://example.com/first.webp", emoji.expandedText)
    }

    @Test
    fun mapsEmojiAlongsideHashtagAndResolvedQuote() {
        val quotedEventId = "22".repeat(32)
        val reference = nevent(quotedEventId)
        val source = note(
            eventId = "11".repeat(32),
            content = "#nostr says :wave:\n\nnostr:$reference",
            tags = listOf(
                listOf("t", "nostr"),
                listOf("emoji", "wave", "https://example.com/wave.webp"),
            ),
        ).apply {
            this.quotedEventId = quotedEventId
            this.quotedNote = note(quotedEventId, "Quoted content")
        }

        val comment = NostrMapper.comment(source, service())

        assertEquals("#nostr says :wave:", comment.text?.displayText)
        assertEquals(
            listOf(AttributedKind.HASH_TAG, AttributedKind.EMOJI),
            comment.text?.elements
                ?.filter { it.kind != AttributedKind.PLAIN }
                ?.map { it.kind },
        )
        assertNotNull(comment.sharedComment)
    }

    @Test
    fun preservesEmojiAfterStripQuotePreservingAttributes() {
        val quotedEventId = "22".repeat(32)
        val reference = nevent(quotedEventId)
        val source = note(
            eventId = "11".repeat(32),
            content = ":wave: hello nostr:$reference",
            tags = listOf(
                listOf("emoji", "wave", "https://example.com/wave.webp"),
            ),
        ).apply {
            this.quotedEventId = quotedEventId
        }

        val comment = NostrMapper.comment(source, service())
        assertEquals(1, emojiElements(comment).size)

        val preserved = NostrMapper.stripQuotePreservingAttributes(
            comment.text!!, quotedEventId
        )

        assertEquals(":wave: hello", preserved.displayText)
        assertEquals(1, preserved.elements.filter { it.kind == AttributedKind.EMOJI }.size)
    }

    @Test
    fun preservesEmojiAndHashtagAfterStripQuotePreservingAttributes() {
        val quotedEventId = "22".repeat(32)
        val reference = nevent(quotedEventId)
        val source = note(
            eventId = "11".repeat(32),
            content = "#nostr says :wave:\n\nnostr:$reference",
            tags = listOf(
                listOf("t", "nostr"),
                listOf("emoji", "wave", "https://example.com/wave.webp"),
            ),
        ).apply {
            this.quotedEventId = quotedEventId
            this.quotedNote = note(quotedEventId, "Quoted content")
        }

        val comment = NostrMapper.comment(source, service())
        val nonPlain = comment.text?.elements?.filter { it.kind != AttributedKind.PLAIN }
        assertEquals(setOf(AttributedKind.HASH_TAG, AttributedKind.EMOJI), nonPlain?.map { it.kind }?.toSet())

        val preserved = NostrMapper.stripQuotePreservingAttributes(
            comment.text!!, quotedEventId
        )

        assertEquals("#nostr says :wave:", preserved.displayText)
        val nonPlainAfter = preserved.elements.filter { it.kind != AttributedKind.PLAIN }
        assertEquals(setOf(AttributedKind.HASH_TAG, AttributedKind.EMOJI), nonPlainAfter.map { it.kind }.toSet())
    }

    @Test
    fun limitsEmojiTagCount() {
        val tags = (0 until 70).map { i ->
            listOf("emoji", "emoji$i", "https://example.com/$i.webp")
        }
        val source = note(
            eventId = "11".repeat(32),
            content = (0 until 70).joinToString(" ") { ":emoji$it:" },
            tags = tags,
        )

        val emojis = NostrMapper.extractEmojis(source)
        assertEquals(64, emojis.size)
    }

    private fun emojiElements(comment: Comment): List<AttributedElement> {
        return comment.text?.elements?.filter { it.kind == AttributedKind.EMOJI }.orEmpty()
    }

    private fun note(
        eventId: String,
        content: String,
        tags: List<List<String>> = emptyList(),
    ): NostrNote {
        return NostrNote().apply {
            event = NostrEvent(
                id = eventId,
                pubkey = "44".repeat(32),
                createdAt = 1_000,
                kind = 1,
                tags = tags,
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

    private fun note1(eventId: String): String {
        return Bech32.encode("note", Hex.decode(eventId))
    }

    private fun service(): Service {
        val account = Account()
        return Service("nostr", account).also {
            account.service = it
        }
    }
}
