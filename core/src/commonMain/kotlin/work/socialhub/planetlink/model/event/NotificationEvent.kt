package work.socialhub.planetlink.model.event

import work.socialhub.planetlink.model.Notification
import kotlin.js.JsExport

@JsExport
class NotificationEvent(
    var notification: Notification
)
