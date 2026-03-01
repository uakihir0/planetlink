package work.socialhub.planetlink.slack.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.slack.action.SlackAuth

object PlanetLinkEx {

    fun PlanetLink.Companion.slack(
        clientId: String? = null,
        clientSecret: String? = null,
    ): SlackAuth {
        return SlackAuth(
            clientId = clientId,
            clientSecret = clientSecret
        )
    }
}
