package work.socialhub.planetlink.x.action

import kotlin.js.JsExport
import work.socialhub.kxweb.XWeb
import work.socialhub.kxweb.XWebFactory
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

@JsExport
class XAuth : ServiceAuth<XWeb> {

    private var authToken: String? = null
    private var csrfToken: String? = null
    private var cookieString: String? = null

    override val accessor: XWeb
        get() = when {
            authToken != null && csrfToken != null ->
                XWebFactory.instance(authToken!!, csrfToken!!)
            cookieString != null ->
                XWebFactory.instanceFromCookieString(cookieString!!)
            else -> XWebFactory.instanceGuest()
        }

    fun accountWithCookies(
        authToken: String,
        csrfToken: String,
    ): Account {
        this.authToken = authToken
        this.csrfToken = csrfToken
        this.cookieString = null
        return account(XWebFactory.instance(authToken, csrfToken), guest = false)
    }

    fun accountWithCookieString(
        cookieString: String,
    ): Account {
        this.cookieString = cookieString
        this.authToken = null
        this.csrfToken = null
        return account(XWebFactory.instanceFromCookieString(cookieString), guest = false)
    }

    fun guestAccount(): Account {
        this.authToken = null
        this.csrfToken = null
        this.cookieString = null
        return account(XWebFactory.instanceGuest(), guest = true)
    }

    private fun account(
        client: XWeb,
        guest: Boolean,
    ): Account {
        return Account().also { account ->
            account.action = XAction(account, this, client, guest)
            account.service = Service("twitter", account).also { service ->
                service.host = "https://x.com"
                service.apiHost = "https://x.com/i/api/graphql"
            }
        }
    }
}
