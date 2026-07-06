package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.discord.model.DiscordIdentify
import work.socialhub.planetlink.discord.model.DiscordPaging
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm
import kotlin.test.Test

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
