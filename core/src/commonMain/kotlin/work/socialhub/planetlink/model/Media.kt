package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.MediaType
import kotlin.js.JsExport

/**
 * Media Model
 * メディアモデル
 */
@JsExport
class Media {

    /** Type of this media */
    var type: MediaType? = null

    /** Link of source media url */
    var sourceUrl: String? = null

    /** Link of preview image url */
    var previewUrl: String? = null

    /** Request header for media. */
    val requestHeader = mutableMapOf<String, String>()
}
