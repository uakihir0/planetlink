package work.socialhub.planetlink.saypip.action

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType

class SaypipCapabilitiesTest {

    private fun action() = SaypipAuth()
        .accountWithAccessToken("token", "refresh")
        .action
        .capabilities()

    @Test
    fun advertisesWhatSaypipHas() {
        val capabilities = action()

        assertTrue(capabilities.isSupported(SocialActionType.GetUserMe))
        assertTrue(capabilities.isSupported(SocialActionType.GetUser))
        assertTrue(capabilities.isSupported(SocialActionType.GetComment))
        assertTrue(capabilities.isSupported(SocialActionType.PostComment))
        assertTrue(capabilities.isSupported(SocialActionType.DeleteComment))
        assertTrue(capabilities.isSupported(SocialActionType.LikeComment))
        assertTrue(capabilities.isSupported(SocialActionType.ReactionComment))
        assertTrue(capabilities.isSupported(SocialActionType.ReportComment))
        assertTrue(capabilities.isSupported(SocialActionType.UpdateProfile))
        assertTrue(capabilities.isSupported(SocialActionType.BlockUser))
        assertTrue(capabilities.isSupported(SocialActionType.MuteUser))

        assertTrue(capabilities.isSupported(TimeLineActionType.HomeTimeLine))
        assertTrue(capabilities.isSupported(TimeLineActionType.UserCommentTimeLine))
        assertTrue(capabilities.isSupported(TimeLineActionType.SearchTimeLine))

        assertTrue(capabilities.isSupported(MessageActionType.GetMessageThread))
        assertTrue(capabilities.isSupported(MessageActionType.GetMessageTimeLine))
        assertTrue(capabilities.isSupported(MessageActionType.PostMessage))

        // The room is a socket, not a poll: a token with `read` may open it.
        assertTrue(
            capabilities.isSupported(
                work.socialhub.planetlink.define.action.StreamActionType.HomeTimeLineStream
            )
        )
    }

    @Test
    fun doesNotAdvertiseWhatSaypipRefuses() {
        val capabilities = action()

        // A friendship is mutual; there is no one-directional follow.
        assertFalse(capabilities.isSupported(SocialActionType.FollowUser))
        assertFalse(capabilities.isSupported(SocialActionType.UnfollowUser))

        // There is no user search and no social graph, by design.
        assertFalse(capabilities.isSupported(work.socialhub.planetlink.define.action.UsersActionType.SearchUsers))
        assertFalse(capabilities.isSupported(work.socialhub.planetlink.define.action.UsersActionType.GetFollowerUsers))

        // No re-sharing, bookmarks, polls or editing.
        assertFalse(capabilities.isSupported(SocialActionType.ShareComment))
        assertFalse(capabilities.isSupported(SocialActionType.BookmarkComment))
        assertFalse(capabilities.isSupported(SocialActionType.VotePoll))
        assertFalse(capabilities.isSupported(SocialActionType.EditComment))

        // The socket carries post IDs, not reactions; a notification stream is not a thing it
        // can answer.
        assertFalse(
            capabilities.isSupported(
                work.socialhub.planetlink.define.action.StreamActionType.NotificationStream
            )
        )
    }
}
