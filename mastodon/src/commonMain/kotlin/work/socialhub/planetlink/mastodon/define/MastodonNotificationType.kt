package work.socialhub.planetlink.mastodon.define

import work.socialhub.planetlink.define.NotificationActionType
import kotlin.js.JsExport

@JsExport
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
    POLL(NotificationActionType.POLL, "poll"),
    ;

    companion object {
        fun of(
            code: String
        ): MastodonNotificationType? {
            return entries.toTypedArray()
                .firstOrNull { it.code == code }
        }

        /**
         * 指定された種別に対応する Mastodon の通知種別を取得
         * (Mastodon の mention は返信も含む)
         * NOTE: Mastodon (and kmastodon) has no quote notification type,
         * so NotificationActionType.QUOTE never matches here.
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

