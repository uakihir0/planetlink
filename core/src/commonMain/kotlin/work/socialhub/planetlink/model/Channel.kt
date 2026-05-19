package work.socialhub.planetlink.model

import kotlinx.datetime.Instant
import kotlin.js.JsExport

/**
 * SNS チャンネル (リスト) 情報
 * SNS Channel (List) Model
 */
@JsExport
open class Channel(
    service: Service
) : Identify(service) {

    var name: String? = null
    var description: String? = null
    var createAt: Instant? = null
    var isPublic: Boolean? = null
}

