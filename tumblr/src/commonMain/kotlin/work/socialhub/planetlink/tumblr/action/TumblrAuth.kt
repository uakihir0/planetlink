package work.socialhub.planetlink.tumblr.action

import work.socialhub.ktumblr.Tumblr
import work.socialhub.ktumblr.TumblrFactory
import work.socialhub.ktumblr.api.request.auth.AuthAuthorizeUrlRequest
import work.socialhub.ktumblr.api.request.auth.AuthOAuth2TokenRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

class TumblrAuth : ServiceAuth<Tumblr> {

    var consumerKey: String? = null
    var consumerSecret: String? = null
    var accessToken: String? = null
    var refreshToken: String? = null

    /** it calls when token is refreshed. */
    var tokenRefreshCallback: (TumblrAuth) -> Unit = {}

    override val accessor: Tumblr
        get() = TumblrFactory.instance(
            checkNotNull(consumerKey)
            { "Set consumer info first." },
            consumerSecret,
            accessToken,
            refreshToken,
        )

    /**
     * Set Consumer Info
     * 申請済みクライアント情報を設定
     */
    fun setConsumerInfo(
        consumerKey: String,
        consumerSecret: String,
    ): TumblrAuth = also {
        it.consumerKey = consumerKey
        it.consumerSecret = consumerSecret
    }

    /**
     * Authentication with AccessToken Secret
     * アクセストークンから生成
     */
    fun accountWithAccessToken(
        accessToken: String,
        refreshToken: String?
    ): Account {
        this.accessToken = accessToken
        this.refreshToken = refreshToken

        return Account().also { acc ->
            acc.action = TumblrAction(acc, this)
            acc.service = Service("tumber", acc)
        }
    }

    /**
     * Get Authorization URL
     * Tumblr の認証ページの URL を取得
     */
    fun authorizationURL(
        redirectUri: String?
    ): String {
        return TumblrFactory.instance(
            checkNotNull(consumerKey)
            { "Set consumer info first." },
            consumerSecret,
        ).auth().authorizeUrl(
            AuthAuthorizeUrlRequest().also {
                it.redirectUri = redirectUri
            })
    }

    /**
     * Authentication with Code
     * 認証してアクセストークンを取得し格納
     */
    fun accountWithVerifier(
        redirectUri: String?,
        code: String
    ): Account {
        val response = TumblrFactory.instance(
            checkNotNull(consumerKey)
            { "Set consumer info first." },
            consumerSecret,
        ).auth().oAuth2Token(
            AuthOAuth2TokenRequest().also {
                it.clientId = consumerKey
                it.clientSecret = consumerSecret
                it.redirectUri = redirectUri
            })

        return accountWithAccessToken(
            checkNotNull(response.data.accessToken)
            { "Tumblr AccessToken is missing" },
            response.data.refreshToken
        )
    }

    fun setTokenRefreshCallback(callback: (TumblrAuth) -> Unit) =
        also { it.tokenRefreshCallback = callback }
}
