package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User

class SlackUser(
    service: Service
) : User(service) {

    var screenName: String? = null
    var team: SlackTeam? = null
    var isBot: Boolean = false
    var displayName: String? = null
}
