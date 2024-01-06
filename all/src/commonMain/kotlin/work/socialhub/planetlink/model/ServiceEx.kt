package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.ServiceType

object ServiceEx {

    /** Is Twitter Account ?  */
    val Service.isTwitter: Boolean
        get() =  (ServiceType.Twitter === type)

    /**
     * Is Mastodon Account ?
     * !! Mastodon compatibility account is always true.
     * !! if pixelfed, use SocialHub#getPixelFedAuth to make account object.
     * !! if pleroma, use SocialHub#getPleromaAuth to make account object.
     */
    val Service.isMastodon: Boolean
        get() = (ServiceType.Mastodon === type)

    val isPixelFed: Boolean
        /**
         * Is PixelFed Account ?
         * !! Mastodon compatibility account is always false.
         * !! Use SocialHub#getPixelFedAuth to make account object.
         */
        get() = (ServiceType.PixelFed === type)

    val isPleroma: Boolean
        /**
         * Is Pleroma Account ?
         * !! Mastodon compatibility account is always false.
         * !! Use SocialHub#getPleromaAuth to make account object.
         */
        get() = (ServiceType.Pleroma === type)

    val isSlack: Boolean
        /** Is Slack Account ?  */
        get() = (ServiceType.Slack === type)

    val isFacebook: Boolean
        /** Is Facebook Account ?  */
        get() = (ServiceType.Facebook === type)

    val isTumblr: Boolean
        /** Is Tumblr Account ?  */
        get() = (ServiceType.Tumblr === type)

    val isMisskey: Boolean
        /** Is Misskey Account ?  */
        get() = (ServiceType.Misskey === type)

    val isBluesky: Boolean
        /** Is Bluesky Account ?  */
        get() = (ServiceType.Bluesky === type)
}