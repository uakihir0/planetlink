package work.socialhub.planetlink.tumblr.define

import kotlin.js.JsExport

/**
 * Tumblr Reaction Type
 * (Action code with alias)
 */
@JsExport
enum class TumblrReactionType(
    vararg val codes: String
) {
    Like("like", "favorite"),
    Reblog("reblog", "retweet", "share"),
    Reply("reply"),
    ;
}