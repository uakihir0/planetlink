package work.socialhub.planetlink.model.common

import kotlin.js.JsExport

@JsExport
enum class AttributedKind {

    // STRINGS
    PLAIN,
    EMOJI,

    // ITEMS
    LINK,
    ACCOUNT,
    HASH_TAG,
    EMAIL,
    PHONE,
    IMAGE,
    VIDEO,

    // ATTRIBUTE
    COLOR,
    STRONG,

    QUOTE,
    OTHER
}
