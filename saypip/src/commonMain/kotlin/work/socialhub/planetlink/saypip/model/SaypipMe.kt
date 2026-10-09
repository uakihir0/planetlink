package work.socialhub.planetlink.saypip.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Service

/**
 * The authenticated account's own state, which Saypip hands out with [SaypipUser] fields beside it.
 *
 * The counters and flags are the product's own answers to "is there anything new" and "is there a
 * mode to offer", so a client does not have to infer them from the screens it happens to have
 * opened.
 */
@JsExport
class SaypipMe(
    service: Service
) : SaypipUser(service) {

    /** How many of the viewer's posts have something new on them. */
    var unreadNotifications: Int = 0

    /** How many live conversations have something the viewer has not read. */
    var unreadConversations: Int = 0

    /** How many people are waiting for an answer to a friend request. */
    var incomingFriendRequests: Int = 0

    /** Whether the friends' timeline is offered at all. */
    var hasFriends: Boolean = false

    /** Whether the connections timeline has a second reason to be offered. */
    var hasWatches: Boolean = false

    /** Whether this account has a live identified persona, and so whether the mode switch exists. */
    var canPostIdentified: Boolean = false

    /** Which post is still asking to be talked to, or null. One ask at a time. */
    var wantsTalkPostId: String? = null

    /** The subjects kept along the top of the timeline, in order, without the `#`. */
    var pinnedSubjects: List<String> = listOf()

    /** Whether this account may open the moderation screens. */
    var isAdmin: Boolean = false

    /** Whether this deployment has somewhere to send feedback to. */
    var canSendFeedback: Boolean = false
}
