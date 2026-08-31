package work.socialhub.planetlink.utils

import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationUtilTest {

    private fun pageable(
        vararg actions: String?
    ): Pageable<Notification> {
        val account = Account()
        val service = Service("test", account)
        return Pageable<Notification>().also { p ->
            p.entities = actions.map { action ->
                Notification(service).also { it.action = action }
            }
            p.paging = Paging(10)
        }
    }

    @Test
    fun keepsEveryNotificationWhenNoActionIsRequested() {
        val filtered = NotificationUtil.filterActions(
            pageable("mention", "like", null), null
        )
        assertEquals(3, filtered.entities.size)
    }

    @Test
    fun keepsOnlyTheRequestedActions() {
        val filtered = NotificationUtil.filterActions(
            pageable("mention", "like", "follow"),
            arrayOf(
                NotificationActionType.MENTION,
                NotificationActionType.FOLLOW,
            )
        )
        assertEquals(
            listOf("mention", "follow"),
            filtered.entities.map { it.action }
        )
    }

    @Test
    fun dropsNotificationsWithoutACommonAction() {
        val filtered = NotificationUtil.filterActions(
            pageable("mention", null),
            arrayOf(NotificationActionType.MENTION)
        )
        assertEquals(
            listOf("mention"),
            filtered.entities.map { it.action }
        )
    }
}
