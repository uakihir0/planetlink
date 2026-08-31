package work.socialhub.planetlink.bluesky.define

import work.socialhub.planetlink.define.NotificationActionType
import kotlin.js.JsExport

/**
 * like, repost, follow, mention, reply, quote
 */
@JsExport
enum class BlueskyNotificationType(
    val action: NotificationActionType,
    val code: String
) {
    MENTION(NotificationActionType.MENTION, "mention"),
    REPLY(NotificationActionType.MENTION, "reply"),
    QUOTE(NotificationActionType.QUOTE, "quote"),
    FOLLOW(NotificationActionType.FOLLOW, "follow"),
    REPOST(NotificationActionType.SHARE, "repost"),
    LIKE(NotificationActionType.LIKE, "like"),
    ;

    companion object {
        fun of(code: String): BlueskyNotificationType? {
            return entries.firstOrNull { it.code == code }
        }

        /**
         * 指定された種別に対応する Bluesky の通知種別を取得
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
