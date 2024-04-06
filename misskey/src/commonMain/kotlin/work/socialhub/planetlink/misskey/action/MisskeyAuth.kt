package work.socialhub.planetlink.misskey.action

import work.socialhub.kmisskey.Misskey
import work.socialhub.kmisskey.MisskeyFactory
import work.socialhub.kmisskey.api.request.CreateAppRequest
import work.socialhub.kmisskey.api.request.GenerateAuthSessionRequest
import work.socialhub.kmisskey.api.request.UserKeyAuthSessionRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

/**
 * Misskey Authorization Functions
 */
class MisskeyAuth(
    var host: String
) : ServiceAuth<Misskey> {

    var clientId: String? = null
    var clientSecret: String? = null
    var accessToken: String? = null

    /**
     * Get Request Token for Misskey
     * Mastodon のリクエストトークンの取得
     */
    override val accessor: Misskey
        get() {
            return accessToken?.let { at ->
                clientSecret?.let { cs ->
                    MisskeyFactory.instance(host, cs, at)
                } ?: MisskeyFactory.instance(host, at)
            } ?: MisskeyFactory.instance(host)
        }

    /**
     * Authentication with AccessToken
     * アクセストークンから生成
     */
    fun accountWithAccessToken(
        accessToken: String
    ): Account {
        this.accessToken = accessToken

        return Account().also { acc ->
            acc.action = MisskeyAction(acc, this)
            acc.service = Service("misskey", acc)
                .also {
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
        description: String,
        callbackUrl: String,
        permissions: Array<String>
    ) {
        val request: CreateAppRequest =
            CreateAppRequest().also {
                it.name = appName
                it.description = description
                it.callbackUrl = callbackUrl
                it.permission = permissions
            }

        val misskey = MisskeyFactory.instance(host)
        val response = misskey.app().createApp(request)

        this.clientId = response.data.id
        this.clientSecret = response.data.secret
    }

    /**
     * Get Authorization URL
     * Misskey の認証ページの URL を取得
     */
    val authorizationURL: String
        get() {
            val misskey = MisskeyFactory.instance(host)
            val response = misskey.auth().sessionGenerate(
                GenerateAuthSessionRequest().also {
                    it.appSecret = clientSecret
                })
            return checkNotNull(response.data.url)
        }

    /**
     * Authentication with Code
     * 認証コードよりアカウントモデルを生成
     */
    fun getAccountWithCode(
        verifier: String
    ): Account {
        val misskey = MisskeyFactory.instance(host)
        val response = misskey.auth().sessionUserKey(
            UserKeyAuthSessionRequest().also {
                it.appSecret = clientSecret
                it.token = verifier
            })
        return accountWithAccessToken(
            checkNotNull(response.data.accessToken)
        )
    }
}
