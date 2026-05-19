package work.socialhub.planetlink.slack.model

import kotlin.js.JsExport

@JsExport
data class SlackTeam(
    var id: String? = null,
    var name: String? = null,
    var domain: String? = null,
    var iconImageUrl: String? = null,
)
