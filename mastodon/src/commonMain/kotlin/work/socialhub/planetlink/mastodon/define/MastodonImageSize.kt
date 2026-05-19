package work.socialhub.planetlink.mastodon.define

import kotlin.js.JsExport

@JsExport
enum class MastodonImageSize(
    val code: String
) {
    Small("small"),
    Original("original"),
}
