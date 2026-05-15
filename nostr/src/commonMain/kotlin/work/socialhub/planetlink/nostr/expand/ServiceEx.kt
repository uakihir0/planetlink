package work.socialhub.planetlink.nostr.expand

import work.socialhub.planetlink.model.Service

object ServiceEx {

    val Service.isNostr: Boolean
        get() = "nostr" == type.lowercase()
}
