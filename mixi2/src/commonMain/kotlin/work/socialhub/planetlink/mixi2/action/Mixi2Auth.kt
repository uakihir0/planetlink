package work.socialhub.planetlink.mixi2.action

import kotlin.js.JsExport
import work.socialhub.kmixi2web.Mixi2Web
import work.socialhub.kmixi2web.Mixi2WebConfig
import work.socialhub.kmixi2web.Mixi2WebFactory
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

/**
 * mixi2 authentication.
 *
 * mixi2 has no official API and no OAuth: the browser session cookie and the
 * `x-auth-key` header, copied from the mixi2 web client, are the credentials.
 * A caller that already drives its own client passes a [Mixi2WebConfig].
 */
@JsExport
class Mixi2Auth : ServiceAuth<Mixi2Web> {

    private var cookie: String? = null
    private var authKey: String? = null
    private var config: Mixi2WebConfig? = null

    override val accessor: Mixi2Web
        get() {
            config?.let { return Mixi2WebFactory.instance(it) }
            return Mixi2WebFactory.instance(
                cookie = cookie.orEmpty(),
                authKey = checkNotNull(authKey) {
                    "Mixi2Auth has no credentials. Call accountWithCredentials first."
                },
            )
        }

    /**
     * Create an account with the web session cookie and the `x-auth-key` header.
     */
    fun accountWithCredentials(
        cookie: String,
        authKey: String,
    ): Account {
        this.cookie = cookie
        this.authKey = authKey
        this.config = null
        return account(Mixi2WebFactory.instance(cookie, authKey))
    }

    /**
     * Create an account with a fully configured mixi2 client.
     */
    fun accountWithConfig(config: Mixi2WebConfig): Account {
        this.config = config
        this.cookie = null
        this.authKey = null
        return account(Mixi2WebFactory.instance(config))
    }

    private fun account(client: Mixi2Web): Account {
        return Account().also { account ->
            account.action = Mixi2Action(account, this, client)
            account.service = Service("mixi2", account).also { service ->
                service.host = Mixi2Mapper.HOST
                service.apiHost = Mixi2Web.DEFAULT_API_BASE_URI
            }
        }
    }
}
