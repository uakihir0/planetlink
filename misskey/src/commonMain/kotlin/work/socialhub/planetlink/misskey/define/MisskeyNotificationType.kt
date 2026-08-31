package work.socialhub.planetlink.misskey.define

import work.socialhub.kmisskey.entity.constant.NotificationType
import work.socialhub.planetlink.define.NotificationActionType
import kotlin.js.JsExport


@JsExport
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

    REACTION(NotificationActionType.REACTION, NotificationType.REACTION.code),
    MENTION(NotificationActionType.MENTION, NotificationType.MENTION.code),
    REPLY(NotificationActionType.MENTION, NotificationType.REPLY.code),
    QUOTE(NotificationActionType.QUOTE, NotificationType.QUOTE.code),
    POLL(NotificationActionType.POLL, NotificationType.POLL_ENDED.code),
    ;

    companion object {
        fun of(
            code: String
        ): MisskeyNotificationType? {
            return entries.toTypedArray()
                .firstOrNull { it.code == code }
        }

        /**
         * 指定された種別に対応する Misskey の通知種別を取得
         * (mention 指定時は mention と reply の両方が対象)
         */
        fun codesOf(
            actions: Array<NotificationActionType>
        ): List<String> {
            return entries
                .filter { actions.contains(it.action) }
                .map { it.code }
        }
    }
}
