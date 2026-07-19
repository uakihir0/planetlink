package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

/**
 * An Identify that carries the channel context Discord message operations need.
 *
 * Discord message endpoints require both a channel id and a message id, but a
 * bare [Identify] only carries the message id. Pass a [DiscordIdentify] (or a
 * [DiscordComment], which already holds its channelId) when calling
 * comment/edit/delete/reaction operations.
 */
@JsExport
class DiscordIdentify(
    service: Service
) : Identify(service) {

    /** Channel id the message belongs to (snowflake). */
    var channelId: String? = null
}
