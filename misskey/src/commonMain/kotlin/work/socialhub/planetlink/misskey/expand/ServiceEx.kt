package work.socialhub.planetlink.misskey.expand

import work.socialhub.planetlink.model.Service

object ServiceEx {

    /** Is Misskey Account ?  */
    val Service.isMisskey: Boolean
        get() = ("misskey" == type.lowercase())
}