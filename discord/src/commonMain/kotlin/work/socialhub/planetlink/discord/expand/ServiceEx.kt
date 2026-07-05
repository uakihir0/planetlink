package work.socialhub.planetlink.discord.expand

import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
object ServiceEx {

    @JsExport.Ignore
    val Service.isDiscord: Boolean
        get() = "discord" == type.lowercase()
}
