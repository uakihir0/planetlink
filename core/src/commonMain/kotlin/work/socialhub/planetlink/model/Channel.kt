package work.socialhub.planetlink.model

/**
 * SNS チャンネル (リスト) 情報
 * SNS Channel (List) Model
 */
open class Channel(
    service: Service
) : Identify(service) {

    var name: String? = null
    var description: String? = null
    var createAt: Instance? = null
    var public: Boolean? = null
}

