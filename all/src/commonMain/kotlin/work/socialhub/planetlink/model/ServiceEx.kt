package work.socialhub.planetlink.model

import kotlin.js.JsExport

@JsExport
object ServiceEx {

    /** Is Twitter Account ?  */
    @JsExport.Ignore
    val Service.isTwitter: Boolean
        get() = ("twitter" == type.lowercase())

    /** Is Slack Account ?  */
    @JsExport.Ignore
    val Service.isSlack: Boolean
        get() = ("slack" == type.lowercase())

    /** Is Facebook Account ?  */
    @JsExport.Ignore
    val Service.isFacebook: Boolean
        get() = ("facebook" == type.lowercase())

    /** Is Tumblr Account ?  */
    @JsExport.Ignore
    val Service.isTumblr: Boolean
        get() = ("tumblr" == type.lowercase())

    /** Is Nostr Account ?  */
    @JsExport.Ignore
    val Service.isNostr: Boolean
        get() = ("nostr" == type.lowercase())

    /** Is Matrix Account ?  */
    @JsExport.Ignore
    val Service.isMatrix: Boolean
        get() = ("matrix" == type.lowercase())
}