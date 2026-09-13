package work.socialhub.planetlink.saypip.define

import kotlin.js.JsExport

/**
 * Saypip reaction type.
 *
 * Saypip reacts with one picture and nothing else, so "like" is a convention rather than a
 * protocol: it is the heart the adapters use for [codes], and any other reaction travels as the
 * emoji itself.
 */
@JsExport
enum class SaypipReactionType(
    vararg val codes: String
) {
    Like("like", "heart", "favorite", "❤️", "❤"),
    ;
}
