package work.socialhub.planetlink.model

import net.socialhub.planetlink.define.ServiceType
import net.socialhub.planetlink.model.RateLimit

/**
 * SNS サービス情報
 * SNS Service Info
 */
class Service(
    var type: ServiceType,
    var account: Account,
) {
    var rateLimit = RateLimit()

    /** Use Only Mastodon  */
    var apiHost: String? = null
}
