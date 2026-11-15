package work.socialhub.planetlink.bluesky.model

import kotlin.js.JsExport

/**
 * A link shown on a Bluesky profile.
 * Bluesky のプロフィールに表示されるリンク
 */
@JsExport
class BlueskyProfileLink {

    /** The app.bsky.actor.link record URI. */
    var uri: String = ""

    /** The app.bsky.actor.link record CID. */
    var cid: String = ""

    /** The link destination. */
    var url: String = ""

    /** Optional label for the link. */
    var title: String? = null

    /** Image URL for the destination site's icon. */
    var icon: String? = null

    /** Label values applied to the link (negated labels are omitted). */
    var labels: List<String> = listOf()
}
