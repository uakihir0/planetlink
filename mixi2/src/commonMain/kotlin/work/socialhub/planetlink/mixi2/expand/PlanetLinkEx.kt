package work.socialhub.planetlink.mixi2.expand

import kotlin.js.JsExport
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.mixi2.action.Mixi2Auth

@JsExport
object PlanetLinkEx {

    /**
     * Create Mixi2Auth
     */
    fun PlanetLink.Companion.mixi2(): Mixi2Auth {
        return Mixi2Auth()
    }
}
