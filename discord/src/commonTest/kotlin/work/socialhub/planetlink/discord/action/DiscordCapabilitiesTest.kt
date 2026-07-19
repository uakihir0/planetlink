package work.socialhub.planetlink.discord.action

import work.socialhub.planetlink.define.action.SocialActionType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordCapabilitiesTest {

    @Test
    fun advertisesReactionsWithoutLikes() {
        val capabilities = DiscordAction.CAPABILITIES

        assertFalse(capabilities.isSupported(SocialActionType.LikeComment))
        assertFalse(capabilities.isSupported(SocialActionType.UnlikeComment))
        assertTrue(capabilities.isSupported(SocialActionType.ReactionComment))
        assertTrue(capabilities.isSupported(SocialActionType.UnreactionComment))
    }
}
