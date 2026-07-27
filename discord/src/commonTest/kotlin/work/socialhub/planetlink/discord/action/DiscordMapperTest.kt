package work.socialhub.planetlink.discord.action

import work.socialhub.kdiscord.entity.Message
import work.socialhub.kdiscord.entity.User
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedKind
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordMapperTest {

    private val service = Service("discord", Account())

    @Test
    fun mapsUserMentionUsingGlobalName() {
        val comment = comment(
            content = "Hello,\n<@407874055874543617>!",
            mentions = arrayOf(
                user(
                    id = "407874055874543617",
                    username = "planetlink",
                    globalName = "Planet Link",
                )
            ),
        )

        assertEquals("Hello,\n@Planet Link!", comment.text?.displayText)
        assertEquals(
            listOf(
                AttributedKind.PLAIN,
                AttributedKind.ACCOUNT,
                AttributedKind.PLAIN,
            ),
            comment.text?.elements?.map { it.kind },
        )
        val mention = comment.text?.elements?.single {
            it.kind == AttributedKind.ACCOUNT
        }
        assertEquals("@Planet Link", mention?.displayText)
        assertEquals("407874055874543617", mention?.expandedText)
    }

    @Test
    fun mapsLegacyMentionUsingUsernameWhenGlobalNameIsMissing() {
        val comment = comment(
            content = "<@!407874055874543617>",
            mentions = arrayOf(
                user(
                    id = "407874055874543617",
                    username = "planetlink",
                )
            ),
        )

        assertEquals("@planetlink", comment.text?.displayText)
        assertEquals(
            AttributedKind.ACCOUNT,
            comment.text?.elements?.single()?.kind,
        )
    }

    @Test
    fun mapsMultipleAndRepeatedMentions() {
        val comment = comment(
            content = "<@1> and <@2>, again <@1>",
            mentions = arrayOf(
                user(id = "1", username = "one", globalName = "One"),
                user(id = "2", username = "two", globalName = "Two"),
            ),
        )

        assertEquals("@One and @Two, again @One", comment.text?.displayText)
        assertEquals(
            listOf("@One", "@Two", "@One"),
            comment.text?.elements
                ?.filter { it.kind == AttributedKind.ACCOUNT }
                ?.map { it.displayText },
        )
    }

    @Test
    fun preservesUnknownMention() {
        val comment = comment(
            content = "Hello <@999> and <@1>",
            mentions = arrayOf(
                user(id = "1", username = "one", globalName = "One")
            ),
        )

        assertEquals("Hello <@999> and @One", comment.text?.displayText)
        assertEquals(
            listOf("@One"),
            comment.text?.elements
                ?.filter { it.kind == AttributedKind.ACCOUNT }
                ?.map { it.displayText },
        )
    }

    @Test
    fun preservesRichTextParsingAroundMention() {
        val comment = comment(
            content = "See https://example.com/docs and <@1> #updates",
            mentions = arrayOf(
                user(id = "1", username = "one", globalName = "One")
            ),
        )

        assertEquals(
            "See https://example.com/docs and @One #updates",
            comment.text?.displayText,
        )
        assertEquals(
            listOf(
                AttributedKind.PLAIN,
                AttributedKind.LINK,
                AttributedKind.PLAIN,
                AttributedKind.ACCOUNT,
                AttributedKind.PLAIN,
                AttributedKind.HASH_TAG,
                AttributedKind.PLAIN,
            ),
            comment.text?.elements?.map { it.kind },
        )
    }

    private fun comment(
        content: String,
        mentions: Array<User>,
    ) = DiscordMapper.comment(
        Message().also {
            it.content = content
            it.mentions = mentions
        },
        userMe = null,
        service = service,
    )

    private fun user(
        id: String,
        username: String,
        globalName: String? = null,
    ) = User().also {
        it.id = id
        it.username = username
        it.globalName = globalName
    }
}
