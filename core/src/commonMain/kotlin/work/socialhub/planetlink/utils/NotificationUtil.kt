package work.socialhub.planetlink.utils

import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable

object NotificationUtil {

    /**
     * 指定された種別の通知のみを残す
     * Keeps only the notifications of the requested action types.
     *
     * For the platforms whose API cannot narrow notifications by type,
     * the filter is applied after the mapping.
     * Notifications without a common action are dropped when the types
     * are specified, since the caller asked for the specific ones.
     */
    fun filterActions(
        pageable: Pageable<Notification>,
        actions: Array<NotificationActionType>?,
    ): Pageable<Notification> {
        if (actions == null) {
            return pageable
        }
        val codes = actions.map { it.code }
        pageable.entities = pageable.entities
            .filter { codes.contains(it.action) }
        return pageable
    }
}
