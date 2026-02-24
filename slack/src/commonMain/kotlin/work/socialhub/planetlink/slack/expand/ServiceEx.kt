package work.socialhub.planetlink.slack.expand

import work.socialhub.planetlink.model.Service

object ServiceEx {

    val Service.isSlack: Boolean
        get() = "slack" == type.lowercase()
}
