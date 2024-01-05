package net.socialhub.planetlink.action.callback.comment

import net.socialhub.planetlink.action.callback.EventCallback
import net.socialhub.planetlink.model.event.NotificationEvent

interface NotificationCommentCallback : EventCallback {
    fun onNotification(reaction: NotificationEvent?)
}
