package work.socialhub.planetlink.mastodon.define

import work.socialhub.planetlink.define.NotificationActionType

enum class MastodonNotificationType(
    val action: NotificationActionType?,
    val code: String
) {
    FOLLOW(
        NotificationActionType.FOLLOW,
        NotificationActionType.FOLLOW.code,
    ),
    FOLLOW_REQUEST(
        NotificationActionType.FOLLOW_REQUEST,
        NotificationActionType.FOLLOW_REQUEST.code,
    ),
    MENTION(
        NotificationActionType.MENTION,
        NotificationActionType.MENTION.code,
    ),

    FAVOURITE(
        NotificationActionType.LIKE,
        "favourite",
    ),
    REBLOG(
        NotificationActionType.SHARE,
        "reblog",
    ),

    STATUS(null, "status"),
    POLL(null, "poll"),
    ;

    companion object {
        fun of(
            code: String
        ): MastodonNotificationType? {
            return entries.toTypedArray()
                .firstOrNull { it.code == code }
        }
    }
}

