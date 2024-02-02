package work.socialhub.planetlink.model

/**
 * SNS サービス情報
 * SNS Service Info
 */
class Service(
    var type: String,
    var account: Account,
) {
    var rateLimit = RateLimit()

    var apiHost: String? = null
    var streamHost: String? = null
}
