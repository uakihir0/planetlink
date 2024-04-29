package work.socialhub.planetlink.tumblr.action

import work.socialhub.ktumblr.Tumblr
import work.socialhub.ktumblr.TumblrFactory
import work.socialhub.ktumblr.api.request.auth.AuthAuthorizeUrlRequest
import work.socialhub.ktumblr.api.request.auth.AuthOAuth2TokenRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

class TumblrAuth(
    var consumerKey: String,
    var consumerSecret: String,
) : ServiceAuth<Tumblr>() {

    var accessToken: String? = null
    var refreshToken: String? = null

    override val accessor: Tumblr
        get() = TumblrFactory.instance(
            consumerKey,
            consumerSecret,
            accessToken,
            refreshToken,
        )

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
            consumerKey,
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
            consumerKey,
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
}
