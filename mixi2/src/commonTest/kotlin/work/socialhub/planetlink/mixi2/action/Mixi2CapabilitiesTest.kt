package work.socialhub.planetlink.mixi2.action

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.mixi2.define.Mixi2ActionType

class Mixi2CapabilitiesTest {

    @Test
    fun advertisesSupportedActions() {
        val capabilities = Mixi2Auth()
            .accountWithCredentials("cookie", "auth-key")
            .action.capabilities()

        assertTrue(capabilities.isSupported(SocialActionType.GetUserMe))
        assertTrue(capabilities.isSupported(SocialActionType.GetUser))
        assertTrue(capabilities.isSupported(SocialActionType.FollowUser))
        assertTrue(capabilities.isSupported(SocialActionType.GetRelationship))
        assertTrue(capabilities.isSupported(SocialActionType.PostComment))
        assertTrue(capabilities.isSupported(SocialActionType.LikeComment))
        assertTrue(capabilities.isSupported(SocialActionType.ReactionComment))
        assertTrue(capabilities.isSupported(SocialActionType.ShareComment))
        assertTrue(capabilities.isSupported(SocialActionType.BookmarkComment))
        assertTrue(capabilities.isSupported(SocialActionType.GetNotification))
        assertTrue(capabilities.isSupported(SocialActionType.MarkNotificationsRead))
        assertTrue(capabilities.isSupported(SocialActionType.GetChannels))
        assertTrue(capabilities.isSupported(TimeLineActionType.HomeTimeLine))
        assertTrue(capabilities.isSupported(TimeLineActionType.ChannelTimeLine))
        assertTrue(capabilities.isSupported(TimeLineActionType.UserBookmarkTimeLine))
        assertTrue(capabilities.isSupported(UsersActionType.GetFollowingUsers))
        assertTrue(capabilities.isSupported(UsersActionType.ChannelUsers))
        assertTrue(capabilities.isSupported(MessageActionType.GetMessageThread))
        assertTrue(capabilities.isSupported(MessageActionType.PostMessage))
        assertTrue(capabilities.isSupported(Mixi2ActionType.RecommendedTimeLine))
        assertTrue(capabilities.isSupported(Mixi2ActionType.FollowingTimeLine))
        assertTrue(capabilities.isSupported(Mixi2ActionType.HashtagTimeLine))
    }

    @Test
    fun hidesUnsupportedActions() {
        val capabilities = Mixi2Auth()
            .accountWithCredentials("cookie", "auth-key")
            .action.capabilities()

        assertFalse(capabilities.isSupported(SocialActionType.EditComment))
        assertFalse(capabilities.isSupported(SocialActionType.VotePoll))
        assertFalse(capabilities.isSupported(SocialActionType.GetSpaces))
        assertFalse(capabilities.isSupported(SocialActionType.CreateList))
        assertFalse(capabilities.isSupported(TimeLineActionType.MentionTimeLine))
    }
}
