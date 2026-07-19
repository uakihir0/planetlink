package work.socialhub.planetlink.x.expand

import kotlin.js.JsExport
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.x.action.XAuth

@JsExport
object PlanetLinkEx {

    fun PlanetLink.Companion.x(): XAuth {
        return XAuth()
    }
}
