package work.socialhub.planetlink.action

import kotlin.test.Test
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.model.Account

class GetUserMeTest : AbstractTest() {

    private fun action(account: Account) {
        dump(account.action.userMe())
    }

    @Test
    fun testBluesky() = action(bluesky())

    @Test
    fun testMisskey() = action(misskey())
}