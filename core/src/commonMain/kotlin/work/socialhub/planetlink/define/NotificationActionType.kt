package work.socialhub.planetlink.define

/**
 * Notification Action Types
 */
enum class NotificationActionType(
    val code: String
) {
    MENTION("mention"),
    FOLLOW("follow"),
    FOLLOW_REQUEST("follow_request"),
    SHARE("share"),
    LIKE("like"),
}
