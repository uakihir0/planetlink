package work.socialhub.planetlink.bluesky.action

import kotlin.test.Test
import kotlin.test.assertTrue
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType

class BlueskyCapabilitiesTest {

    @Test
    fun capabilities_includeBookmarkTimeline() {
        val capabilities = BlueskyAction.CAPABILITIES

        assertTrue(capabilities.isSupported(SocialActionType.GetUserBookmarks))
        assertTrue(capabilities.isSupported(TimeLineActionType.UserBookmarkTimeLine))
    }
}
