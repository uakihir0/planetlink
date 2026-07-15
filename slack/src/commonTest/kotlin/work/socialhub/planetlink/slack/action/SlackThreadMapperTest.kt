package work.socialhub.planetlink.slack.action

import work.socialhub.kslack.api.methods.response.conversations.ConversationsListResponse
import work.socialhub.kslack.entity.Conversation
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlackThreadMapperTest {

    private fun service(): Service {
        val account = Account()
        return Service("slack", account)
    }

    private fun user(id: String, service: Service): User {
        return User(service).apply {
            this.id = ID(id)
            name = id
        }
    }

    private fun conversation(
        id: String,
        isOpen: Boolean = true,
        isArchived: Boolean = false,
        isIm: Boolean = true,
        user: String? = null,
        members: Array<String>? = null,
    ): Conversation {
        return Conversation().apply {
            this.id = id
            this.isOpen = isOpen
            this.isArchived = isArchived
            this.isIm = isIm
            this.isMpim = !isIm
            this.user = user
            this.members = members
        }
    }

    @Test
    fun testThreadMemberIdsUseDmCounterpart() {
        val dm = conversation(id = "D1", user = "U_OTHER")
        val selfDm = conversation(id = "D2", user = "U_ME")
        val mpim = conversation(
            id = "G1",
            isIm = false,
            members = arrayOf("U_ME", "U_ONE", "U_TWO"),
        )

        assertEquals(listOf("U_OTHER"), SlackMapper.threadMemberIds(dm, "U_ME"))
        assertEquals(listOf("U_ME"), SlackMapper.threadMemberIds(selfDm, "U_ME"))
        assertEquals(
            listOf("U_ONE", "U_TWO"),
            SlackMapper.threadMemberIds(mpim, "U_ME"),
        )
    }

    @Test
    fun testGroupThreadFallsBackToSlackGroupId() {
        val group = conversation(id = "G1", isIm = false).apply {
            isMpim = false
        }

        assertTrue(SlackMapper.isGroupThread(group))
    }

    @Test
    fun testThreadsRetainSelfDmAndExcludeOtherClosedConversations() {
        val service = service()
        val response = ConversationsListResponse().apply {
            channels = arrayOf(
                conversation(id = "D_OPEN", user = "U_OTHER"),
                conversation(id = "D_SELF", isOpen = false, user = "U_ME"),
                conversation(id = "D_CLOSED", isOpen = false, user = "U_CLOSED"),
                conversation(id = "D_ARCHIVED", isArchived = true, user = "U_ME"),
            )
        }
        val other = user("U_OTHER", service)
        val self = user("U_ME", service)

        val threads = SlackMapper.threads(
            response = response,
            memberMap = mapOf(
                "D_OPEN" to listOf("U_OTHER"),
                "D_SELF" to listOf("U_ME"),
            ),
            historyMap = emptyMap(),
            accountMap = mapOf("U_OTHER" to other, "U_ME" to self),
            userMeId = "U_ME",
            service = service,
        )

        assertEquals(listOf("D_OPEN", "D_SELF"), threads.map { it.id?.value<String>() })
        assertEquals(listOf("U_OTHER"), threads[0].users?.map { it.id?.value<String>() })
        assertEquals(listOf("U_ME"), threads[1].users?.map { it.id?.value<String>() })
    }

    @Test
    fun testThreadPagingIsTerminal() {
        val pageable = Pageable<Thread>().apply {
            entities = emptyList()
            paging = SlackMapper.threadPaging(Paging(100))
        }

        assertFalse(pageable.paging!!.isHasNew)
        assertFalse(pageable.paging!!.isHasPast)
    }
}
