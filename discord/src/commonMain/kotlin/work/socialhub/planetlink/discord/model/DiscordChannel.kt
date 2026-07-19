package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

/** Discord チャンネルモデル */
@JsExport
class DiscordChannel(
    service: Service
) : Channel(service) {

    /** Guild id this channel belongs to (snowflake); null for DMs. */
    var guildId: String? = null

    /** Discord channel type (0 = text, 1 = DM, 2 = voice, ...). */
    var type: Int? = null

    /** Topic of the channel. */
    var topic: String? = null

    /** Sorting position of the channel. */
    var position: Int? = null
}
