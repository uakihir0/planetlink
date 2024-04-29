package work.socialhub.planetlink.mastodon.action

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import work.socialhub.kmastodon.Mastodon
import work.socialhub.kmastodon.MastodonFactory
import work.socialhub.kmastodon.api.request.apps.AppsRegisterApplicationRequest
import work.socialhub.kmastodon.api.request.oauth.OAuthAuthorizationUrlRequest
import work.socialhub.kmastodon.api.request.oauth.OAuthIssueAccessTokenWithAuthorizationCodeRequest
import work.socialhub.kmastodon.api.request.oauth.OAuthRefreshAccessTokenRequest
import work.socialhub.kmastodon.domain.Service
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service as PLService

/**
 * Mastodon Authorization Functions
 */
class MastodonAuth(
    var host: String,
    val type: String,
) : ServiceAuth<Mastodon>() {

    var clientId: String? = null
    var clientSecret: String? = null
    var accessToken: String? = null
    var refreshToken: String? = null
    var expiredAt: Instant? = null

    /**
     * Get Request Token for Mastodon
     * Mastodon のリクエストトークンの取得
     */
    override val accessor: Mastodon
        get() = MastodonFactory.instance(
            this.host,
            this.accessToken ?: "",
            Service.from(type),
        )

    /**
     * Authentication with AccessToken
     * アクセストークンから生成
     */
    fun accountWithAccessToken(
        accessToken: String?,
        refreshToken: String?,
        expiredAt: Instant?,
    ): Account {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiredAt = expiredAt

        return Account().also { acc ->
            acc.action = MastodonAction(acc, this)
            acc.service = PLService(type, acc).also {
                it.apiHost = host
                it.streamHost = host
            }
        }
    }

    /**
     * Set Client Info
     * 申請済みクライアント情報を設定
     */
    fun setClientInfo(
        clientId: String,
        clientSecret: String,
    ) {
        this.clientId = clientId
        this.clientSecret = clientSecret
    }

    /**
     * Request Client Application
     * クライアント情報を申請して設定
     */
    fun requestClientApplication(
        appName: String,
        website: String,
        redirectUris: String,
        scopes: String,
    ) {
        val credential = accessor.apps().registerApplication(
            AppsRegisterApplicationRequest().also {
                it.name = appName
                it.website = website
                it.redirectUris = redirectUris
                it.scopes = scopes
            })

        this.clientId = credential.data.clientId
        this.clientSecret = credential.data.clientSecret
    }

    /**
     * Get Authorization URL
     * Mastodon の認証ページの URL を取得
     */
    fun authorizationURL(
        redirectUri: String,
        scopes: String,
    ): String {
        return accessor.oauth().authorizationUrl(
            OAuthAuthorizationUrlRequest().also {
                it.clientId = clientId
                it.redirectUri = redirectUri
                it.scopes = scopes
            }).data
    }

    /**
     * Authentication with Code
     * 認証コードよりアカウントモデルを生成
     */
    fun accountWithCode(
        redirectUri: String,
        code: String,
    ): Account {
        val accessToken = accessor.oauth()
            .issueAccessTokenWithAuthorizationCode(
                OAuthIssueAccessTokenWithAuthorizationCodeRequest().also {
                    it.clientId = clientId
                    it.clientSecret = clientSecret
                    it.redirectUri = redirectUri
                    it.code = code
                }
            )

        return accountWithAccessToken(
            accessToken.data.accessToken,
            accessToken.data.refreshToken,
            accessToken.data.expiresIn?.let { expireAt(it) }
        )
    }

    /**
     * Refresh AccessToken with RefreshToken.
     * トークン情報を更新
     */
    fun refreshToken(): Account {
        val accessToken = accessor.oauth().refreshAccessToken(
            OAuthRefreshAccessTokenRequest().also {
                it.clientId = clientId
                it.clientSecret = clientSecret
                it.refreshToken = refreshToken
            })

        return accountWithAccessToken(
            accessToken.data.accessToken,
            accessToken.data.refreshToken,
            accessToken.data.expiresIn?.let { expireAt(it) }
        )
    }

    private fun expireAt(
        expireInSec: Int
    ): Instant {
        val now = Clock.System.now()
        val newSec = now.epochSeconds + expireInSec
        return Instant.fromEpochSeconds(newSec)
    }

    companion object {
        /** Show token on Mastodon WebUI  */
        const val REDIRECT_NONE: String = "urn:ietf:wg:oauth:2.0:oob"
    }
}
