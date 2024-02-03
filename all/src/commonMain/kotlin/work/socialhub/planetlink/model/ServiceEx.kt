package work.socialhub.planetlink.model

object ServiceEx {

    /** Is Twitter Account ?  */
    val Service.isTwitter: Boolean
        get() = ("twitter" == type.lowercase())

    /**
     * Is Mastodon Account ?
     * !! Mastodon compatibility account is always true.
     * !! if pixelfed, use SocialHub#getPixelFedAuth to make account object.
     * !! if pleroma, use SocialHub#getPleromaAuth to make account object.
     */
    val Service.isMastodon: Boolean
        get() = ("mastodon" == type.lowercase())

    /**
     * Is PixelFed Account ?
     * !! Mastodon compatibility account is always false.
     * !! Use SocialHub#getPixelFedAuth to make account object.
     */
    val Service.isPixelFed: Boolean
        get() = ("pixelfed" == type.lowercase())

    /**
     * Is Pleroma Account ?
     * !! Mastodon compatibility account is always false.
     * !! Use SocialHub#getPleromaAuth to make account object.
     */
    val Service.isPleroma: Boolean
        get() = ("pleroma" == type.lowercase())

    /** Is Slack Account ?  */
    val Service.isSlack: Boolean
        get() = ("slack" == type.lowercase())

    /** Is Facebook Account ?  */
    val Service.isFacebook: Boolean
        get() = ("facebook" == type.lowercase())

    /** Is Tumblr Account ?  */
    val Service.isTumblr: Boolean
        get() = ("tumblr" == type.lowercase())

    /** Is Misskey Account ?  */
    val Service.isMisskey: Boolean
        get() = ("misskey" == type.lowercase())


}