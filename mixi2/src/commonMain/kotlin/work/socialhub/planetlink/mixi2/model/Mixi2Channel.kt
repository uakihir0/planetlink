package work.socialhub.planetlink.mixi2.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Service

/**
 * mixi2 community, mapped to a channel.
 *
 * A mixi2 community is a group with its own timeline and members, so it is the
 * list a caller browses and the id [work.socialhub.planetlink.action.AccountAction.channelTimeLine]
 * reads, the same shape as a Slack channel.
 */
@JsExport
class Mixi2Channel(
    service: Service
) : Channel(service) {

    /** Whether the community is archived. */
    var isArchived: Boolean = false

    /** Number of members. */
    var memberCount: Int? = null

    /** Raw community type (topic or event). */
    var communityType: String? = null

    /** Raw access level (public or approval-required). */
    var accessLevel: String? = null
}
