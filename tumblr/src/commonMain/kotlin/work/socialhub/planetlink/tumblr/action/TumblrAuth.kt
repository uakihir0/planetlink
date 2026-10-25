package work.socialhub.planetlink.tumblr.action

import kotlin.js.JsExport
import work.socialhub.ktumblr.Tumblr
import work.socialhub.ktumblr.TumblrFactory
import work.socialhub.ktumblr.api.request.auth.AuthAuthorizeUrlRequest
import work.socialhub.ktumblr.api.request.auth.AuthOAuth2TokenRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

@JsExport
class TumblrAuth : ServiceAuth<Tumblr> {

    var consumerKey: String? = null
    var consumerSecret: String? = null
    var accessToken: String? = null
    var refreshToken: String? = null

    /** it calls when token is refreshed. */
    @JsExport.Ignore
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
    @Throws(Throwable::class)
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
    @Throws(Throwable::class)
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
    @Throws(Throwable::class)
    suspend fun authorizationURL(
        redirectUri: String?,
        state: String? = null,
        scope: String? = null,
    ): String {
        return TumblrFactory.instance(
            checkNotNull(consumerKey)
            { "Set consumer info first." },
            consumerSecret,
        ).auth().authorizeUrl(
            AuthAuthorizeUrlRequest().also {
                it.redirectUri = redirectUri
                if (state != null) {
                    it.state = state
                }
                if (scope != null) {
                    it.scope = scope
                }
            })
    }

    /**
     * Authentication with Code
     * 認証してアクセストークンを取得し格納
     */
    @Throws(Throwable::class)
    suspend fun accountWithVerifier(
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
                it.code = code
            })

        return accountWithAccessToken(
            checkNotNull(response.data.accessToken)
            { "Tumblr AccessToken is missing" },
            response.data.refreshToken
        )
    }

    @JsExport.Ignore
    @Throws(Throwable::class)
    fun setTokenRefreshCallback(callback: (TumblrAuth) -> Unit) =
        also { it.tokenRefreshCallback = callback }
}
