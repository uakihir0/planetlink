package work.socialhub.planetlink.action

import org.junit.jupiter.api.Test
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump

class GetUserMeTest : AbstractTest() {

    @Test
    fun testBluesky() {
        dump(bluesky().action.userMe())
    }

    @Test
    fun testMisskey() {
        dump(misskey().action.userMe())
    }
}