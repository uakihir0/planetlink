package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Space
import kotlin.js.JsExport

/** Discord スペース (guild / サーバー) モデル */
@JsExport
class DiscordSpace(
    service: Service
) : Space(service) {

    /** True if the current user is the owner of the guild. */
    var owner: Boolean? = null

    /** Approximate member count (populated only when requested with counts). */
    var approximateMemberCount: Int? = null
}
