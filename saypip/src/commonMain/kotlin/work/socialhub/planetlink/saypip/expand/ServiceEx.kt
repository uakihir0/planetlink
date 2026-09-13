package work.socialhub.planetlink.saypip.expand

import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
object ServiceEx {

    @JsExport.Ignore
    val Service.isSaypip: Boolean
        get() = ("saypip" == type.lowercase())
}
