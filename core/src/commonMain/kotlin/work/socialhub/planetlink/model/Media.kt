package work.socialhub.planetlink.model

import net.socialhub.planetlink.define.MediaType

/**
 * Media Model
 * メディアモデル
 */
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
