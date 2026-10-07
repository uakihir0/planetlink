package work.socialhub.planetlink.mixi2.expand

import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
object ServiceEx {

    @JsExport.Ignore
    val Service.isMixi2: Boolean
        get() = ("mixi2" == type.lowercase())
}
