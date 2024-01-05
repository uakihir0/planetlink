package net.socialhub.planetlink.model

import net.socialhub.planetlink.define.MediaType

/**
 * Media Model
 * メディアモデル
 */
class Media : java.io.Serializable {
    //region // Getter&Setter
    /** Type of this media  */
    var type: MediaType? = null

    /** Link of source media url  */
    var sourceUrl: String? = null

    //endregion
    /** Link of preview image url  */
    var previewUrl: String? = null

    val requestHeader: Map<String, String>
        /** Get request header for authorize  */
        get() = java.util.HashMap<String, String>()
}
