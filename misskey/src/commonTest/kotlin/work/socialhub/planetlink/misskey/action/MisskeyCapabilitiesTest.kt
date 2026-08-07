package work.socialhub.planetlink.misskey.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.misskey.define.MisskeyActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

class MisskeyCapabilitiesTest {

    private fun account(): Account {
        val account = Account()
        account.action = MisskeyAction(account, MisskeyAuth("https://example.com"))
        account.service = Service("misskey", account)
        return account
    }

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

    @Test
    fun socialTimeLineRequestRoundTripsFromRawString() {
        val request = MisskeyRequest(account())
        val raw = request.socialTimeLine.toRawString()

        val restored = request.fromRawString(checkNotNull(raw))
        assertNotNull(restored)
        assertEquals(MisskeyActionType.SocialTimeLine, restored.actionType)
        assertEquals(request.socialTimeLine.actionType, restored.actionType)
    }
}
