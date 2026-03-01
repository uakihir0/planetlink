package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dumpUsers
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test

class GetUsersTest {

    @Nested
    inner class Followings : AbstractTest() {
        private suspend fun Account.act() = dumpUsers(
            action.followingUsers(action.userMe(), Paging(100))
        )

        @Test
        fun testBluesky() = runTest { bluesky().act() }

        @Test
        fun testMisskey() = runTest { misskey().act() }

        @Test
        fun testMastodon() = runTest { mastodon().act() }

        @Test
        fun testTumblr() = runTest { tumblr().act() }
    }
}
