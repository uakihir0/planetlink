package work.socialhub.planetlink.action.callback.comment

import work.socialhub.planetlink.model.event.NotificationEvent
import work.socialhub.planetlink.action.callback.EventCallback

interface NotificationCommentCallback : EventCallback {
    fun onNotification(reaction: NotificationEvent?)
}
