package work.socialhub.planetlink.misskey.define

import misskey4j.entity.contant.NotificationType
import work.socialhub.planetlink.define.NotificationActionType


enum class MisskeyNotificationType(
    val action: NotificationActionType?,
    val code: String
) {
    FOLLOW(
        NotificationActionType.FOLLOW,
        NotificationType.FOLLOW.code,
    ),
    RENOTE(
        NotificationActionType.SHARE,
        NotificationType.RENOTE.code,
    ),

    REACTION(null, NotificationType.REACTION.code),
    MENTION(null, NotificationType.MENTION.code),
    POLL(null, NotificationType.POLL_VOTE.code),
    ;

    companion object {
        fun of(
            code: String
        ): MisskeyNotificationType {
            return entries.toTypedArray()
                .first { it.code == code }
        }
    }
}
