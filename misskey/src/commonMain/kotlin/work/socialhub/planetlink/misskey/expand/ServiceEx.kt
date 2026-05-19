package work.socialhub.planetlink.misskey.expand

import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
object ServiceEx {

    /** Is Misskey Account ?  */
    @JsExport.Ignore
    val Service.isMisskey: Boolean
        get() = ("misskey" == type.lowercase())
}