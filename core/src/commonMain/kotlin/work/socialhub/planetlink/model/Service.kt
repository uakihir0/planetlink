package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.ServiceType

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
