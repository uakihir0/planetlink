package work.socialhub.planetlink.model

import kotlinx.datetime.Instant

/**
 * SNS チャンネル (リスト) 情報
 * SNS Channel (List) Model
 */
open class Channel(
    service: Service
) : Identify(service) {

    var name: String? = null
    var description: String? = null
    var createAt: Instant? = null
    var isPublic: Boolean? = null
}

