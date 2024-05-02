package work.socialhub.planetlink.tumblr.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.tumblr.action.TumblrAuth

object PlanetLinkEx {

    /**
     * Create TumblrAuth
     */
    fun PlanetLink.Companion.tumblr(
    ): TumblrAuth {
        return TumblrAuth()
    }
}