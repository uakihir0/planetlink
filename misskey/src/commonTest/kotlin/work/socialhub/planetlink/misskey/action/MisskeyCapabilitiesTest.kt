package work.socialhub.planetlink.misskey.action

import kotlin.test.Test
import kotlin.test.assertTrue
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.misskey.define.MisskeyActionType

class MisskeyCapabilitiesTest {

    @Test
    fun capabilities_includeBookmarkTimeline() {
        val capabilities = MisskeyAction.CAPABILITIES

        assertTrue(capabilities.isSupported(SocialActionType.GetUserBookmarks))
        assertTrue(capabilities.isSupported(TimeLineActionType.UserBookmarkTimeLine))
    }

    @Test
    fun capabilities_includeSocialTimeline() {
        val capabilities = MisskeyAction.CAPABILITIES

        assertTrue(capabilities.isSupported(MisskeyActionType.SocialTimeLine))
    }
}
