package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service

class SlackIdentify(
    service: Service
) : Identify(service) {

    var channel: String? = null
}
