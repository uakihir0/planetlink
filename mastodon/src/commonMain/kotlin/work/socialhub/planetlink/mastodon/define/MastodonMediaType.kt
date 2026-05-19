package work.socialhub.planetlink.mastodon.define

import kotlin.js.JsExport

@JsExport
enum class MastodonMediaType(
    vararg val codes: String
) {
    Image("image"),
    Video("video", "gifv"),
    Unknown("unknown");

    companion object {
        fun of(code: String): MastodonMediaType {
            return entries.toTypedArray().firstOrNull { type ->
                type.codes.any { it.equals(code, ignoreCase = true) }
            } ?: Unknown
        }
    }
}