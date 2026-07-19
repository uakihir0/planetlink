package work.socialhub.planetlink.x.expand

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Service

@JsExport
object ServiceEx {

    @JsExport.Ignore
    val Service.isX: Boolean
        get() = type.lowercase() in setOf("x", "twitter")
}
