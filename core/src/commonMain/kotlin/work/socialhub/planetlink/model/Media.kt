package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.MediaType
import kotlin.js.JsExport

/**
 * Media Model
 * メディアモデル
 */
@JsExport
open class Media {

    /** Type of this media */
    var type: MediaType? = null

    /** Link of source media url */
    var sourceUrl: String? = null

    /** Link of preview image url */
    var previewUrl: String? = null

    /** Alt text / media description (where the platform provides it) */
    var description: String? = null

    /** BlurHash for a placeholder preview (where the platform provides it) */
    var blurhash: String? = null

    /** Request header for media. */
    val requestHeader = mutableMapOf<String, String>()
}
