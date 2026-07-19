package work.socialhub.planetlink.discord.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.discord.action.DiscordAuth
import kotlin.js.JsExport

@JsExport
object PlanetLinkEx {

    /**
     * Create a Discord auth entry point.
     * `apiHost` overrides the default Discord REST endpoint.
     */
    fun PlanetLink.Companion.discord(
        apiHost: String? = null,
    ): DiscordAuth {
        return DiscordAuth(
            apiHost = apiHost,
        )
    }
}
