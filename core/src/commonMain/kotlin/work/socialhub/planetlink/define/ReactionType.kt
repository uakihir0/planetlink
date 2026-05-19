package work.socialhub.planetlink.define

import kotlin.js.JsExport

@JsExport
enum class ReactionType(
    val codes: String
) {
    Like("like"),
    Share("share"),
    Reply("reply"),
}
