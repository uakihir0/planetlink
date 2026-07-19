package work.socialhub.planetlink.matrix.action

import work.socialhub.planetlink.define.action.SocialActionType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatrixCapabilitiesTest {

    @Test
    fun advertisesReactionsWithoutLikes() {
        val capabilities = MatrixAction.CAPABILITIES

        assertFalse(capabilities.isSupported(SocialActionType.LikeComment))
        assertFalse(capabilities.isSupported(SocialActionType.UnlikeComment))
        assertTrue(capabilities.isSupported(SocialActionType.ReactionComment))
        assertFalse(capabilities.isSupported(SocialActionType.UnreactionComment))
    }
}
