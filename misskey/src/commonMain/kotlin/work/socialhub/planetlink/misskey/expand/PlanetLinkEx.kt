package work.socialhub.planetlink.misskey.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.misskey.action.MisskeyAuth
import kotlin.js.JsExport

@JsExport
object PlanetLinkEx {

    /**
     * Create MisskeyAuth
     */
    fun PlanetLink.Companion.misskey(
        apiHost: String,
    ): MisskeyAuth {
        return MisskeyAuth(apiHost)
    }
}