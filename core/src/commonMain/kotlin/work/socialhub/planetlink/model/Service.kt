package net.socialhub.planetlink.model

import net.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.Account

/**
 * SNS サービス情報
 * SNS Service Info
 */
class Service(type: ServiceType, account: Account) : java.io.Serializable {
    private var account: Account

    private var type: ServiceType
    private var rateLimit: RateLimit

    // endregion
    /** Use Only Mastodon  */
    var apiHost: String? = null

    /**
     * Constructor
     */
    init {
        this.type = type
        this.account = account
        this.rateLimit = RateLimit()
    }


    val isTwitter: Boolean
        /** Is Twitter Account ?  */
        get() = (ServiceType.Twitter === type)

    val isMastodon: Boolean
        /**
         * Is Mastodon Account ?
         * !! Mastodon compatibility account is always true.
         * !! if pixelfed, use SocialHub#getPixelFedAuth to make account object.
         * !! if pleroma, use SocialHub#getPleromaAuth to make account object.
         */
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

    // region // Getter&Setter
    fun getAccount(): Account {
        return account
    }

    fun setAccount(account: Account) {
        this.account = account
    }

    fun getType(): ServiceType {
        return type
    }

    fun setType(type: ServiceType) {
        this.type = type
    }

    fun getRateLimit(): RateLimit {
        return rateLimit
    }

    fun setRateLimit(rateLimit: RateLimit) {
        this.rateLimit = rateLimit
    }
}
