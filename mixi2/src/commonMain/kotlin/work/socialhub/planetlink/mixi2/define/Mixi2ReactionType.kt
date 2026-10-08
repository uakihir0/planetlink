package work.socialhub.planetlink.mixi2.define

import kotlin.js.JsExport

/**
 * mixi2 reaction type.
 *
 * mixi2 reacts with a like (one heart) or with a stamp. A stamp is addressed by
 * its stamp id rather than by a picture, so [codes] recognises the names of the
 * like and every other reaction travels to `addStampToPost` as the stamp id.
 */
@JsExport
enum class Mixi2ReactionType(
    vararg val codes: String
) {
    Like("like", "heart", "favorite", "❤️", "❤"),
    ;
}
