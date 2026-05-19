package work.socialhub.planetlink.bluesky.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.bluesky.action.BlueskyAuth
import kotlin.js.JsExport

@JsExport
object PlanetLinkEx {

    /**
     * Create BlueskyAuth
     */
    fun PlanetLink.Companion.bluesky(
        apiHost: String,
        streamHost: String? = null,
    ): BlueskyAuth {

        return BlueskyAuth(
            apiHost = apiHost,
            streamHost = streamHost ?: apiHost
        )
    }
}