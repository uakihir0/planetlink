package work.socialhub.planetlink.model

import kotlin.time.Instant
import kotlin.js.JsExport

/**
 * SNS スペース (上位コンテナ) 情報
 * SNS Space (top-level container) Model
 *
 * A Space is the container that holds channels: a Discord guild, a Slack
 * workspace, a Matrix space, etc. On platforms without such a hierarchy
 * (Mastodon, Misskey) the authenticated user is the single implicit Space.
 * Fetch channels of a specific Space via [work.socialhub.planetlink.action.AccountAction.channels].
 */
@JsExport
open class Space(
    service: Service
) : Identify(service) {

    var name: String? = null
    var description: String? = null
    var createAt: Instant? = null

    /** Icon (avatar) URL of the space, if any. */
    var iconUrl: String? = null
}
