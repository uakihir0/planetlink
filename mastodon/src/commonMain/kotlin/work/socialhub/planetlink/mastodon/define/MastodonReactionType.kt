package work.socialhub.planetlink.mastodon.define

import kotlin.js.JsExport

/**
 * Mastodon Reaction Type
 * (Action code with alias)
 */
@JsExport
enum class MastodonReactionType(
    vararg val codes: String
) {
    Favorite("favorite", "like"),
    Reblog("reblog", "retweet", "share"),
    Reply("reply"),
    ;
}