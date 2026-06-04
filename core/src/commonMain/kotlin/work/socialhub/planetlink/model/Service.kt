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

    /** Actual instance host (e.g. misskey.io) */
    var host: String? = null

    /** API host (e.g. proxy URL). Falls back to host if not set. */
    var apiHost: String? = null

    /** Streaming host. Falls back to host if not set. */
    var streamHost: String? = null
}
