package work.socialhub.planetlink.action

import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dumpComments
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test

class UserTimelineTest : AbstractTest() {

    private fun Account.act() {
        dumpComments(
            action.userCommentTimeLine(
                action.userMe(),
                Paging(100),
            )
        )
    }

    @Test
    fun testBluesky() = bluesky().act()

    @Test
    fun testMisskey() = misskey().act()

    @Test
    fun testMastodon() = mastodon().act()

    @Test
    fun testTumblr() = tumblr().act()
}