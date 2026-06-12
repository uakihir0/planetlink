package work.socialhub.planetlink.action.callback.comment

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.event.NotificationEvent
import kotlin.js.JsExport

@JsExport
interface NotificationCommentCallback : EventCallback {
    fun onNotification(reaction: NotificationEvent?)
}
