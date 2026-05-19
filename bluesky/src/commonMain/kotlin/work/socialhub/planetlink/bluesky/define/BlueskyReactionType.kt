package work.socialhub.planetlink.bluesky.define

import kotlin.js.JsExport

/**
 * Bluesky Reaction Type
 * (Action code with alias)
 */
@JsExport
enum class BlueskyReactionType(
    vararg codes: String
) {
    Like("like", "favorite"),
    Repost("repost", "retweet", "share"),
    Reply("reply"),
    ;

    val codes = listOf(*codes)
}
