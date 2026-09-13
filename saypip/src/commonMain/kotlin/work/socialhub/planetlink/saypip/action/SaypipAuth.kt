package work.socialhub.planetlink.saypip.action

import kotlin.js.JsExport
import work.socialhub.ksaypip.Saypip
import work.socialhub.ksaypip.SaypipFactory
import work.socialhub.ksaypip.auth.OAuthContext
import work.socialhub.ksaypip.auth.SaypipAuthConfig
import work.socialhub.ksaypip.auth.SaypipAuthFactory
import work.socialhub.ksaypip.auth.api.entity.oauth.BuildAuthorizationUrlRequest
import work.socialhub.ksaypip.auth.api.entity.oauth.OAuthAuthorizationCodeTokenRequest
import work.socialhub.ksaypip.auth.api.entity.oauth.OAuthRefreshTokenRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

/**
 * Saypip authentication.
 *
 * Saypip is one deployment with one reader's view, and third-party clients reach it through its
 * OAuth 2.1 authorization server with PKCE: [authorizationURL] sends the reader to decide, and
 * [accountWithVerifier] exchanges the returned code. An application that already holds a token
 * comes in through [accountWithAccessToken].
 *
 * A refresh token arrives only when the reader agreed to `offline_access`; where it did,
 * [refreshAccessToken] renews the access token and [tokenRefreshCallback] tells the caller to
 * store the new pair.
 */
@JsExport
class SaypipAuth : ServiceAuth<Saypip> {

    var host: String = "https://saypip.app"

    /** The client ID an operator's register issued. */
    var clientId: String? = null

    /** The client secret, for a confidential client. Never set for a native one. */
    var clientSecret: String? = null

    /** Where the authorization server returns the code. */
    var redirectUri: String? = null

    var accessToken: String? = null
    var refreshToken: String? = null

    /** it calls when token is refreshed. */
    @JsExport.Ignore
    var tokenRefreshCallback: (SaypipAuth) -> Unit = {}

    private val oauthContext = OAuthContext()

    override val accessor: Saypip
        get() = SaypipFactory.instance(
            host,
            accessToken ?: "",
        )

    /**
     * Set Consumer Info
     * 申請済みクライアント情報を設定
     */
    fun setConsumerInfo(
        clientId: String,
        clientSecret: String? = null,
    ): SaypipAuth = also {
        it.clientId = clientId
        it.clientSecret = clientSecret
    }

    /**
     * Authentication with AccessToken
     * アクセストークンから生成
     */
    fun accountWithAccessToken(
        accessToken: String,
        refreshToken: String? = null,
    ): Account {
        this.accessToken = accessToken
        this.refreshToken = refreshToken

        return Account().also { account ->
            account.action = SaypipAction(account, this)
            account.service = Service("saypip", account).also { service ->
                service.host = host
                service.apiHost = host
            }
        }
    }

    /**
     * Get Authorization URL
     * Saypip の認証ページの URL を取得
     */
    suspend fun authorizationURL(
        redirectUri: String? = null,
    ): String {
        this.redirectUri = redirectUri ?: this.redirectUri
        return client().oauth().buildAuthorizationUrl(
            oauthContext,
            BuildAuthorizationUrlRequest(),
        )
    }

    /**
     * Authentication with Code
     * 認証してアクセストークンを取得し格納
     */
    suspend fun accountWithVerifier(
        redirectUri: String? = null,
        code: String,
    ): Account {
        val tokens = client(redirectUri).oauth().authorizationCodeToken(
            oauthContext,
            OAuthAuthorizationCodeTokenRequest().also {
                it.code = code
            },
        ).data

        return accountWithAccessToken(
            tokens.accessToken,
            tokens.refreshToken,
        )
    }

    /**
     * Whether the current account can renew its own access token.
     */
    fun canRefresh(): Boolean {
        return refreshToken != null
    }

    /**
     * Refresh access token
     * アクセストークンを更新
     */
    fun refreshAccessToken(
        redirectUri: String? = null,
    ) {
        val refreshToken = checkNotNull(this.refreshToken) {
            "Set refresh token first."
        }

        val tokens = client(redirectUri).oauth().refreshToken(
            oauthContext,
            OAuthRefreshTokenRequest().also {
                it.refreshToken = refreshToken
            },
        ).data

        accessToken = tokens.accessToken
        tokens.refreshToken?.let { this.refreshToken = it }
        tokenRefreshCallback(this)
    }

    @JsExport.Ignore
    fun setTokenRefreshCallback(callback: (SaypipAuth) -> Unit) =
        also { it.tokenRefreshCallback = callback }

    private fun client(redirectUri: String? = null): work.socialhub.ksaypip.auth.SaypipAuth {
        return SaypipAuthFactory.instance(
            SaypipAuthConfig(
                baseUrl = host,
                clientId = checkNotNull(clientId) {
                    "Set client info first."
                },
                clientSecret = clientSecret,
                redirectUri = redirectUri ?: this.redirectUri,
            ),
        )
    }
}
