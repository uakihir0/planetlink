package work.socialhub.planetlink.bluesky.define

import work.socialhub.planetlink.define.NotificationActionType

/**
 * like, repost, follow, mention, reply, quote
 */
enum class BlueskyNotificationType(
    val action: NotificationActionType,
    val code: String
) {
    MENTION(NotificationActionType.MENTION, "mention"),
    REPLY(NotificationActionType.MENTION, "reply"),
    FOLLOW(NotificationActionType.FOLLOW, "follow"),
    REPOST(NotificationActionType.SHARE, "repost"),
    LIKE(NotificationActionType.LIKE, "like"),
    ;

    companion object {
        fun of(code: String): BlueskyNotificationType? {
            return entries.firstOrNull { it.code == code }
        }
    }
}
