package work.socialhub.planetlink.mastodon.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.mastodon.action.MastodonAuth

object PlanetLinkEx {

    /**
     * Create MastodonAuth
     */
    fun PlanetLink.Companion.mastodon(
        host: String,
        type: String = "MASTODON",
    ): MastodonAuth {
        return MastodonAuth(
            host = host,
            type = type
        )
    }

    /**
     * Create PleromaAuth
     */
    fun PlanetLink.Companion.pleroma(
        host: String,
    ): MastodonAuth {
        return MastodonAuth(
            host = host,
            type = "PLEROMA"
        )
    }

    /**
     * Create PixelFedAuth
     */
    fun PlanetLink.Companion.pixelfed(
        host: String,
    ): MastodonAuth {
        return MastodonAuth(
            host = host,
            type = "PIXELFED"
        )
    }
}