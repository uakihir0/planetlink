package work.socialhub.planetlink.saypip.expand

import kotlin.js.JsExport
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.saypip.action.SaypipAuth

@JsExport
object PlanetLinkEx {

    /**
     * Create SaypipAuth
     */
    fun PlanetLink.Companion.saypip(
        host: String = "https://saypip.app",
    ): SaypipAuth {
        return SaypipAuth().also {
            it.host = host
        }
    }
}
