package work.socialhub.planetlink.define

import kotlin.js.JsExport

/**
 * Notification Action Types
 */
@JsExport
enum class NotificationActionType(
    val code: String
) {
    /**
     * Mentions and replies
     * メンションと返信
     * (Platforms which distinguish replies from mentions map both of them here)
     */
    MENTION("mention"),

    /**
     * Quotes
     * 引用
     * (Platforms which have no quote notification type never return this)
     */
    QUOTE("quote"),
    FOLLOW("follow"),
    FOLLOW_REQUEST("follow_request"),
    SHARE("share"),
    LIKE("like"),
    REACTION("reaction"),
    POLL("poll"),
}
