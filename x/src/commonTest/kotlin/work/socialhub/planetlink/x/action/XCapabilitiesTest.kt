package work.socialhub.planetlink.x.action

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.x.define.XActionType

class XCapabilitiesTest {

    @Test
    fun authenticatedAccountIsReadOnly() {
        val capabilities = XAuth()
            .accountWithCookies("auth-token", "csrf-token")
            .action.capabilities()

        assertTrue(capabilities.isSupported(TimeLineActionType.HomeTimeLine))
        assertTrue(capabilities.isSupported(XActionType.RecommendedTimeLine))
        assertTrue(capabilities.isSupported(SocialActionType.GetUserBookmarks))

        assertFalse(capabilities.isSupported(SocialActionType.PostComment))
        assertFalse(capabilities.isSupported(SocialActionType.DeleteComment))
        assertFalse(capabilities.isSupported(SocialActionType.LikeComment))
        assertFalse(capabilities.isSupported(SocialActionType.ShareComment))
        assertFalse(capabilities.isSupported(SocialActionType.BookmarkComment))
        assertFalse(capabilities.isSupported(SocialActionType.FollowUser))
    }

    @Test
    fun guestAccountOnlyAdvertisesPublicReads() {
        val capabilities = XAuth()
            .guestAccount()
            .action.capabilities()

        assertTrue(capabilities.isSupported(SocialActionType.GetUser))
        assertTrue(capabilities.isSupported(SocialActionType.GetComment))
        assertTrue(capabilities.isSupported(XActionType.GetTrends))

        assertFalse(capabilities.isSupported(SocialActionType.GetUserMe))
        assertFalse(capabilities.isSupported(TimeLineActionType.HomeTimeLine))
        assertFalse(capabilities.isSupported(TimeLineActionType.SearchTimeLine))
    }
}
