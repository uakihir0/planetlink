package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread
import kotlin.js.JsExport

/** Discord スレッド (DM チャンネル) モデル */
@JsExport
class DiscordThread(
    service: Service
) : Thread(service) {

    /** The underlying DM channel id (snowflake). */
    var channelId: String? = null
}
