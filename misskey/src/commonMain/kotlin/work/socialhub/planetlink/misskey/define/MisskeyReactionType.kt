package work.socialhub.planetlink.misskey.define

import kotlin.js.JsExport

@JsExport
enum class MisskeyReactionType(
    vararg val codes: String
) {
    Favorite("favorite", "like"),
    Renote("renote", "retweet", "reblog", "share"),
    Reply("reply"),
    ;
}
