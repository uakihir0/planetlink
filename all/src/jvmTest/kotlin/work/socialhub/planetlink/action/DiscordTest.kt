package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.discord.model.DiscordIdentify
import work.socialhub.planetlink.discord.model.DiscordPaging
import work.socialhub.planetlink.discord.model.DiscordSpace
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Live tests for the Discord adapter. They require DISCORD_USER_TOKEN (and,
 * for channel operations, DISCORD_CHANNEL_ID) in secrets.json. Each test skips
 * gracefully when the required credentials are not present.
 */
class DiscordTest : AbstractTest() {

    private fun hasToken(): Boolean =
        !config?.get("DISCORD_USER_TOKEN").isNullOrBlank()

    private fun channelId(): String? =
        config?.get("DISCORD_CHANNEL_ID")?.takeIf { it.isNotBlank() }

    @Test
    fun testUserMe() = runTest {
        if (!hasToken()) return@runTest
        dump(discord().action.userMe())
    }

    @Test
    fun testSpaces() = runTest {
        if (!hasToken()) return@runTest
        // Single request; proves there is no fan-out at the space level.
        val spaces = discord().action.spaces(
            DiscordPaging().also { it.count = 200 }
        )
        assertTrue(spaces.entities.isNotEmpty())
        spaces.entities.forEach {
            val space = it as DiscordSpace
            println("${space.id?.value<String>()} > ${space.name} (${space.approximateMemberCount}) ${space.iconUrl}")
        }
    }

    @Test
    fun testSpaceChannels() = runTest {
        if (!hasToken()) return@runTest
        val account = discord()
        // 1 request for the guild list, then 1 request for the chosen guild's
        // channels — never a channel fetch per guild.
        val spaces = account.action.spaces(DiscordPaging())
        val space = spaces.entities.firstOrNull() ?: return@runTest
        val channels = account.action.channels(space, DiscordPaging())
        println("=== channels of ${space.name} ===")
        channels.entities.forEach { println("  ${it.id?.value<String>()} > ${it.name}") }
    }

    @Test
    fun testChannelTimeLine() = runTest {
        if (!hasToken()) return@runTest
        val channelId = channelId() ?: return@runTest
        val account = discord()
        val id = DiscordIdentify(Service(account.service.type, account)).also {
            it.id = ID(channelId)
        }
        val timeline = account.action.channelTimeLine(
            id, DiscordPaging().also { it.count = 20 }
        )
        timeline.entities.forEach { println("${it.user?.name}: ${it.text?.displayText}") }
    }

    @Test
    fun testPostComment() = runTest {
        if (!hasToken()) return@runTest
        val channelId = channelId() ?: return@runTest
        val account = discord()
        account.action.postComment(
            CommentForm().also {
                it.text = "Hello from planetlink Discord adapter!"
                it.addParam("channel", channelId)
            }
        )
    }
}
