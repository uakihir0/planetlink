package work.socialhub.planetlink.action

import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.UsersRequest
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestActionRawStringTest {

    private fun account(): Account {
        val account = Account()
        account.service = Service("test", account)
        account.action = object : AccountAction {
            override suspend fun searchUsers(query: String, paging: Paging): Pageable<User> {
                return Pageable()
            }

            override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
                return Pageable()
            }
        }
        return account
    }

    @Test
    fun restoresSearchUsers() {
        val action = RequestActionImpl(account())
        val raw = """{"action":"SearchUsers","params":{"query":"fixture"}}"""

        val request = assertNotNull(action.fromRawString(raw))

        assertTrue(request is UsersRequest)
        assertEquals(UsersActionType.SearchUsers, request.actionType)
        assertEquals("fixture", request.raw?.params?.get("query"))
    }

    @Test
    fun restoresChannelUsers() {
        val action = RequestActionImpl(account())
        val raw = """{"action":"ChannelUsers","params":{"id":"Sgeneral"}}"""

        val request = assertNotNull(action.fromRawString(raw))

        assertTrue(request is UsersRequest)
        assertEquals(UsersActionType.ChannelUsers, request.actionType)
        assertEquals("Sgeneral", request.raw?.params?.get("id"))
    }

    @Test
    fun restoresTimelineRequests() {
        val action = RequestActionImpl(account())
        val raw = """{"action":"HomeTimeLine","params":{}}"""

        val request = assertNotNull(action.fromRawString(raw))

        assertTrue(request is CommentsRequest)
        assertEquals(TimeLineActionType.HomeTimeLine, request.actionType)
        assertNull(request.raw?.params?.get("query"))
    }

    @Test
    fun keepsSearchTimeLineAsCommentsRequest() {
        val action = RequestActionImpl(account())
        val raw = """{"action":"SearchTimeLine","params":{"query":"fixture"}}"""

        val request = assertNotNull(action.fromRawString(raw))

        assertTrue(request is CommentsRequest)
        assertEquals(TimeLineActionType.SearchTimeLine, request.actionType)
    }
}
