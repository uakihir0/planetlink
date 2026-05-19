package work.socialhub.planetlink.model.request

import kotlin.js.JsExport


@JsExport
class MediaForm(
    /** Media Data  */
    var data: ByteArray,
    /** Media File Name  */
    var name: String,
) {
    /** Copy this object  */
    fun copy(): MediaForm {
        return MediaForm(data, name)
    }
}
