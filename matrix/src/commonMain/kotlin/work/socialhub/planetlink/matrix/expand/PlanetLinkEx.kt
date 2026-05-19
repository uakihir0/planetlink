package work.socialhub.planetlink.matrix.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.matrix.action.MatrixAuth
import kotlin.js.JsExport

@JsExport
object PlanetLinkEx {

    fun PlanetLink.Companion.matrix(
        host: String,
        accessToken: String? = null,
    ): MatrixAuth {
        return MatrixAuth(host = host, accessToken = accessToken)
    }
}
