package work.socialhub.planetlink.slack.expand

import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
object ServiceEx {

    @JsExport.Ignore
    val Service.isSlack: Boolean
        get() = "slack" == type.lowercase()
}
