package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.ServiceType

object ServiceEx {

    /** Is Twitter Account ?  */
    val Service.isTwitter: Boolean
        get() = (ServiceType.Twitter === type)

    /**
     * Is Mastodon Account ?
     * !! Mastodon compatibility account is always true.
     * !! if pixelfed, use SocialHub#getPixelFedAuth to make account object.
     * !! if pleroma, use SocialHub#getPleromaAuth to make account object.
     */
    val Service.isMastodon: Boolean
        get() = (ServiceType.Mastodon == type)

    /**
     * Is PixelFed Account ?
     * !! Mastodon compatibility account is always false.
     * !! Use SocialHub#getPixelFedAuth to make account object.
     */
    val Service.isPixelFed: Boolean
        get() = (ServiceType.PixelFed == type)

    /**
     * Is Pleroma Account ?
     * !! Mastodon compatibility account is always false.
     * !! Use SocialHub#getPleromaAuth to make account object.
     */
    val Service.isPleroma: Boolean
        get() = (ServiceType.Pleroma == type)

    /** Is Slack Account ?  */
    val Service.isSlack: Boolean
        get() = (ServiceType.Slack == type)

    /** Is Facebook Account ?  */
    val Service.isFacebook: Boolean
        get() = (ServiceType.Facebook == type)

    /** Is Tumblr Account ?  */
    val Service.isTumblr: Boolean
        get() = (ServiceType.Tumblr == type)

    /** Is Misskey Account ?  */
    val Service.isMisskey: Boolean
        get() = (ServiceType.Misskey == type)

    /** Is Bluesky Account ?  */
    val Service.isBluesky: Boolean
        get() = (ServiceType.Bluesky == type)
}