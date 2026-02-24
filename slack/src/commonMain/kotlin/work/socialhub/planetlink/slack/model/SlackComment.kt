package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Service

class SlackComment(
    service: Service
) : Comment(service) {

    var channelId: String? = null
    var threadTs: String? = null
    var replyCount: Int = 0
}
