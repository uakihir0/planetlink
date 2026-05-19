package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * SNS サービス情報
 * SNS Service Info
 */
@JsExport
class Service(
    var type: String,
    var account: Account,
) {
    var rateLimit = RateLimit()

    var apiHost: String? = null
    var streamHost: String? = null
}
