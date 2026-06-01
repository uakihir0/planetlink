package work.socialhub.planetlink.define

import kotlin.js.JsExport

/**
 * Notification Action Types
 */
@JsExport
enum class NotificationActionType(
    val code: String
) {
    MENTION("mention"),
    FOLLOW("follow"),
    FOLLOW_REQUEST("follow_request"),
    SHARE("share"),
    LIKE("like"),
    REACTION("reaction"),
}
