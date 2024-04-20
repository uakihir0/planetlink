package work.socialhub.planetlink.action

import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dumpComments
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test

class HomeTimelineTest : AbstractTest() {

    private fun Account.act() =
        dumpComments(action.homeTimeLine(Paging(100)))

    @Test
    fun testBluesky() = bluesky().act()

    @Test
    fun testMisskey() = misskey().act()

    @Test
    fun testMastodon() = mastodon().act()
}