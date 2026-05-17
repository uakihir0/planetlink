package work.socialhub.planetlink.nostr.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.nostr.action.NostrAuth

object PlanetLinkEx {

    fun PlanetLink.Companion.nostr(
        relays: List<String>,
        nsec: String? = null,
    ): NostrAuth {
        return NostrAuth(relays = relays, nsec = nsec)
    }
}
