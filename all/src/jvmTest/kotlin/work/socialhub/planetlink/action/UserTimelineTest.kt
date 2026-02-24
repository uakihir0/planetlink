package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dumpComments
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test

class UserTimelineTest : AbstractTest() {

    private suspend fun Account.act() {
        dumpComments(
            action.userCommentTimeLine(
                action.userMe(),
                Paging(100),
            )
        )
    }

    @Test
    fun testBluesky() = runTest { bluesky().act() }

    @Test
    fun testMisskey() = runTest { misskey().act() }

    @Test
    fun testMastodon() = runTest { mastodon().act() }

    @Test
    fun testTumblr() = runTest { tumblr().act() }
}
