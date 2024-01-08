package work.socialhub.planetlink.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Instance
import work.socialhub.planetlink.model.Service

/**
 * SNS チャンネル (リスト) 情報
 * SNS Channel (List) Model
 */
class Channel(
    service: Service
) : Identify(service) {

    var name: String? = null
    var description: String? = null
    var createAt: Instance? = null
    var public: Boolean? = null
}

