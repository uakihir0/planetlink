package work.socialhub.planetlink.action

import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test

class HomeTimelineTest : AbstractTest() {

    private fun action(account: Account) {
        dump(account.action.homeTimeLine(Paging(100)))
    }

    @Test
    fun testBluesky() = action(bluesky())

    @Test
    fun testMisskey() = action(misskey())
}