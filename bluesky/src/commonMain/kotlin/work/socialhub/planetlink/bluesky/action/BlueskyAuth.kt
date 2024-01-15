package work.socialhub.planetlink.bluesky.action

import io.ktor.http.*
import work.socialhub.kbsky.Bluesky
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

class BlueskyAuth(
    host: String
) : ServiceAuth<Bluesky> {

    /** Client Objects */
    var bluesky: Bluesky? = null

    var host: String
    var identifier: String? = null
    var password: String? = null

    init {
        var uri = host
        if (!host.startsWith("http")) {
            uri = "https://$host"
        }
        try {
            val obj = Url(uri)
            this.host = "${obj.protocol}://${obj.host}/"
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "invalid host: $host"
            )
        }
    }

    /**
     * Get Access Token for Bluesky
     * Bluesky のアクセストークンの取得
     */
    fun getAccountWithIdentifyAndPassword(
        identifier: String,
        password: String,
    ): Account {

        this.identifier = identifier
        this.password = password
        this.bluesky = BlueskyFactory.instance(this.host)

        val account = Account()
        val type = ServiceType.Bluesky
        val service = Service(type, account)
        service.apiHost = this.host
        // TODO: SetStreamHost

        account.setAction(BlueskyAction(account, this))
        account.setService(service)
        return account
    }


    override val accessor: Bluesky
        get() {
            return checkNotNull(bluesky) {
                "bluesky client is not initialized."
            }
        }
}
