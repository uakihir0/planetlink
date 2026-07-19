package work.socialhub.planetlink.model.request

import kotlin.js.JsExport


@JsExport
class MediaForm(
    /** Media Data  */
    var data: ByteArray,
    /** Media File Name  */
    var name: String,
) {
    /** Alt text / media description (where the platform supports it)  */
    var description: String? = null

    /** Width of this media (pixels) if known. Sent as aspect ratio where supported. */
    var width: Int? = null

    /** Height of this media (pixels) if known. Sent as aspect ratio where supported. */
    var height: Int? = null

    /** Copy this object  */
    fun copy(): MediaForm {
        return MediaForm(data, name).also {
            it.description = description
            it.width = width
            it.height = height
        }
    }
}
