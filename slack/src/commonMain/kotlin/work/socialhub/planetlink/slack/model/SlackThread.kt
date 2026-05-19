package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread
import kotlin.js.JsExport

@JsExport
class SlackThread(
    service: Service
) : Thread(service) {

    var channelId: String? = null
    var latestTs: String? = null
}
