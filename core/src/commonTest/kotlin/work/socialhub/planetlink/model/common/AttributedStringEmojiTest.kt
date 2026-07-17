package work.socialhub.planetlink.model.common

import work.socialhub.planetlink.model.Emoji
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributedStringEmojiTest {

    @Test
    fun preservesMatchedShortcodeInEmojiElements() {
        val imageUrl = "https://example.com/kodomo.webp"
        val emoji = Emoji().also {
            it.addShortCode("kodomo")
            it.imageUrl = imageUrl
        }
        val attributed = AttributedString.plain(
            "before :kodomo: after :kodomo:"
        )

        attributed.addEmojiElement(listOf(emoji))

        assertEquals(
            "before :kodomo: after :kodomo:",
            attributed.displayText
        )
        val emojiElements = attributed.elements.filter {
            it.kind == AttributedKind.EMOJI
        }
        assertEquals(2, emojiElements.size)
        for (element in emojiElements) {
            assertEquals(":kodomo:", element.displayText)
            assertEquals(imageUrl, element.expandedText)
        }
    }
}
