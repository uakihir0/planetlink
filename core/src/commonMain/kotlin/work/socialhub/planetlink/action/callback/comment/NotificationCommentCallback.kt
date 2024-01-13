package work.socialhub.planetlink.action.callback.comment

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.event.NotificationEvent

interface NotificationCommentCallback : EventCallback {
    fun onNotification(reaction: NotificationEvent?)
}
