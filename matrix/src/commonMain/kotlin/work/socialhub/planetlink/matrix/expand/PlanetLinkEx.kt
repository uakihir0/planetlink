package work.socialhub.planetlink.matrix.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.matrix.action.MatrixAuth

object PlanetLinkEx {

    fun PlanetLink.Companion.matrix(
        host: String,
        accessToken: String? = null,
    ): MatrixAuth {
        return MatrixAuth(host = host, accessToken = accessToken)
    }
}
