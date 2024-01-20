package work.socialhub.planetlink.bluesky.action

import io.ktor.http.*
import work.socialhub.kbsky.Bluesky
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

class BlueskyAuth(
    var apiHost: String,
    var streamHost: String? = null,
) : ServiceAuth<Bluesky> {

    /** Client Objects */
    var bluesky: Bluesky? = null

    var identifier: String? = null
    var password: String? = null

    init {
        apiHost = toUrl(apiHost, "https")
        streamHost?.let {
            streamHost = toUrl(it, "wss")
        }
    }

    private fun toUrl(
        host: String,
        protocol: String,
    ): String {

        var url = host
        if (!host.startsWith(protocol)) {
            url = "${protocol}://$host"
        }
        try {
            val obj = Url(url)
            return "${obj.protocol}://${obj.host}/"
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
    fun accountWithIdentifyAndPassword(
        identifier: String,
        password: String,
    ): Account {

        this.identifier = identifier
        this.password = password
        this.bluesky = BlueskyFactory.instance(apiHost)

        return Account().also { acc ->
            acc.action = BlueskyAction(acc, this)
            acc.service = Service(ServiceType.Bluesky, acc)
                .also {
                    it.apiHost = apiHost
                    it.streamHost = streamHost
                }
        }
    }


    override val accessor: Bluesky
        get() {
            return checkNotNull(bluesky) {
                "bluesky client is not initialized."
            }
        }
}
