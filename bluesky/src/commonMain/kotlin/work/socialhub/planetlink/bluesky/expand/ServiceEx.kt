package work.socialhub.planetlink.bluesky.expand

import work.socialhub.planetlink.model.Service

object ServiceEx {

    /** Is Bluesky Account ?  */
    val Service.isBluesky: Boolean
        get() = ("bluesky" == type.lowercase())
}