package work.socialhub.planetlink.mastodon.expand

import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
object ServiceEx {

    /**
     * Is Mastodon Account ?
     * !! Mastodon compatibility account is always true.
     * !! if pixelfed, use SocialHub#getPixelFedAuth to make account object.
     * !! if pleroma, use SocialHub#getPleromaAuth to make account object.
     */
    @JsExport.Ignore
    val Service.isMastodon: Boolean
        get() = ("mastodon" == type.lowercase())

    /**
     * Is PixelFed Account ?
     * !! Mastodon compatibility account is always false.
     * !! Use SocialHub#getPixelFedAuth to make account object.
     */
    @JsExport.Ignore
    val Service.isPixelFed: Boolean
        get() = ("pixelfed" == type.lowercase())

    /**
     * Is Pleroma Account ?
     * !! Mastodon compatibility account is always false.
     * !! Use SocialHub#getPleromaAuth to make account object.
     */
    @JsExport.Ignore
    val Service.isPleroma: Boolean
        get() = ("pleroma" == type.lowercase())
}