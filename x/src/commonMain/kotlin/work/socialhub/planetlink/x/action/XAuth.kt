package work.socialhub.planetlink.x.action

import kotlin.js.JsExport
import work.socialhub.kxweb.XWeb
import work.socialhub.kxweb.XWebFactory
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

@JsExport
class XAuth : ServiceAuth<XWeb> {

    private lateinit var client: XWeb
    internal var guest: Boolean = false
        private set

    override val accessor: XWeb
        get() = client

    fun accountWithCookies(
        authToken: String,
        csrfToken: String,
    ): Account {
        client = XWebFactory.instance(authToken, csrfToken)
        guest = false
        return account()
    }

    fun accountWithCookieString(
        cookieString: String,
    ): Account {
        client = XWebFactory.instanceFromCookieString(cookieString)
        guest = false
        return account()
    }

    fun guestAccount(): Account {
        client = XWebFactory.instanceGuest()
        guest = true
        return account()
    }

    private fun account(): Account {
        return Account().also { account ->
            account.action = XAction(account, this)
            account.service = Service("twitter", account).also { service ->
                service.host = "https://x.com"
                service.apiHost = "https://x.com/i/api/graphql"
            }
        }
    }
}
