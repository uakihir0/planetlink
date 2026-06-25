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

    /** Copy this object  */
    fun copy(): MediaForm {
        return MediaForm(data, name).also {
            it.description = description
        }
    }
}
