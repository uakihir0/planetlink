package work.socialhub.planetlink.bluesky.expand

import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
object ServiceEx {

    /** Is Bluesky Account ?  */
    @JsExport.Ignore
    val Service.isBluesky: Boolean
        get() = ("bluesky" == type.lowercase())
}