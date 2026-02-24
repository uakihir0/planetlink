package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Service

class SlackChannel(
    service: Service
) : Channel(service) {

    var isChannel: Boolean = false
    var isGroup: Boolean = false
    var isIm: Boolean = false
    var isMpim: Boolean = false
    var isPrivate: Boolean = false
    var isArchived: Boolean = false
    var isGeneral: Boolean = false
    var isShared: Boolean = false
    var isOrgShared: Boolean = false

    var creator: String? = null
    var topic: String? = null
    var purpose: String? = null
    var numMembers: Int? = null
}
