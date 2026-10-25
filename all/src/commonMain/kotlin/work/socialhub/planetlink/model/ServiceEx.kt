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
        // Tumblr アダプタの Service.type は "tumber" という綴りになっている。
        // web 版が "tumber" に依存しているので、両方を受け付ける。
        get() = ("tumblr" == type.lowercase() || "tumber" == type.lowercase())

    /** Is Nostr Account ?  */
    @JsExport.Ignore
    val Service.isNostr: Boolean
        get() = ("nostr" == type.lowercase())

    /** Is Matrix Account ?  */
    @JsExport.Ignore
    val Service.isMatrix: Boolean
        get() = ("matrix" == type.lowercase())

    /** Is Saypip Account ?  */
    @JsExport.Ignore
    val Service.isSaypip: Boolean
        get() = ("saypip" == type.lowercase())
}