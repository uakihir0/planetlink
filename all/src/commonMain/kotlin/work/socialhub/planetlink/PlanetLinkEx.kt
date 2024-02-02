package work.socialhub.planetlink

import work.socialhub.planetlink.bluesky.action.BlueskyAuth

object PlanetLinkEx {

    fun PlanetLink.Companion.bluesky(
        apiHost: String,
        streamHost: String,
    ) = BlueskyAuth(
        apiHost = apiHost,
        streamHost = streamHost,
    )
}