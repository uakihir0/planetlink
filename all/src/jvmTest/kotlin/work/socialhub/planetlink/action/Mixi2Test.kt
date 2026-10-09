package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.PrintClass.dumpComments
import work.socialhub.planetlink.PrintClass.dumpUsers
import work.socialhub.planetlink.mixi2.action.Mixi2Action
import work.socialhub.planetlink.mixi2.model.Mixi2Paging

/**
 * Live tests for the mixi2 adapter. They require MIXI2_COOKIE and
 * MIXI2_AUTH_KEY in secrets.json and skip gracefully without them. Every test
 * is read-only.
 */
class Mixi2Test : AbstractTest() {

    private fun hasCredentials(): Boolean =
        !config?.get("MIXI2_COOKIE").isNullOrBlank() &&
            !config?.get("MIXI2_AUTH_KEY").isNullOrBlank()

    @Test
    fun testUserMe() = runTest {
        if (!hasCredentials()) return@runTest
        dump(mixi2().action.userMe())
    }

    @Test
    fun testHomeTimeline() = runTest {
        if (!hasCredentials()) return@runTest
        dumpComments(mixi2().action.homeTimeLine(Mixi2Paging(20)))
    }

    @Test
    fun testNotification() = runTest {
        if (!hasCredentials()) return@runTest
        val notifications = mixi2().action.notification(Mixi2Paging(20))
        notifications.entities.forEach {
            println("${it.action}: ${it.users?.firstOrNull()?.name} ${it.createAt}")
        }
    }

    @Test
    fun testFollowingUsers() = runTest {
        if (!hasCredentials()) return@runTest
        val action = mixi2().action as Mixi2Action
        val me = action.userMe()
        dumpUsers(action.followingUsers(me, Mixi2Paging(20)))
    }

    @Test
    fun testChannels() = runTest {
        if (!hasCredentials()) return@runTest
        val action = mixi2().action as Mixi2Action
        val me = action.userMe()
        val channels = action.channels(me, Mixi2Paging(20))
        channels.entities.forEach {
            println("${it.id?.value<String>()} > ${it.name}")
        }
    }

    @Test
    fun testChannelTimeLine() = runTest {
        if (!hasCredentials()) return@runTest
        val communityId = config?.get("MIXI2_COMMUNITY_ID")?.takeIf { it.isNotBlank() }
            ?: return@runTest
        val action = mixi2().action as Mixi2Action
        val id = work.socialhub.planetlink.model.Identify(
            action.account.service,
            work.socialhub.planetlink.model.ID(communityId),
        )
        dumpComments(action.channelTimeLine(id, Mixi2Paging(20)))
    }
}
