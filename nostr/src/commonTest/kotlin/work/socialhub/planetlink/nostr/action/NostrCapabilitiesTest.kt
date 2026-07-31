package work.socialhub.planetlink.nostr.action

import kotlin.test.Test
import kotlin.test.assertTrue
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType

class NostrCapabilitiesTest {

    @Test
    fun advertisesRecentlyAddedKnostrFeatures() {
        val capabilities = NostrAction.CAPABILITIES

        assertTrue(capabilities.isSupported(SocialActionType.GetUserBookmarks))
        assertTrue(capabilities.isSupported(SocialActionType.BookmarkComment))
        assertTrue(capabilities.isSupported(SocialActionType.UnbookmarkComment))
        assertTrue(capabilities.isSupported(SocialActionType.VotePoll))
        assertTrue(capabilities.isSupported(SocialActionType.GetChannels))
        assertTrue(capabilities.isSupported(SocialActionType.CreateList))
        assertTrue(capabilities.isSupported(TimeLineActionType.UserBookmarkTimeLine))
        assertTrue(capabilities.isSupported(TimeLineActionType.ChannelTimeLine))
    }
}
