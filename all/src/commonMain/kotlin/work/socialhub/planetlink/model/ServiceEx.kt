package work.socialhub.planetlink.model

object ServiceEx {

    /** Is Twitter Account ?  */
    val Service.isTwitter: Boolean
        get() = ("twitter" == type.lowercase())

    /** Is Slack Account ?  */
    val Service.isSlack: Boolean
        get() = ("slack" == type.lowercase())

    /** Is Facebook Account ?  */
    val Service.isFacebook: Boolean
        get() = ("facebook" == type.lowercase())

    /** Is Tumblr Account ?  */
    val Service.isTumblr: Boolean
        get() = ("tumblr" == type.lowercase())
}