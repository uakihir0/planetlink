package work.socialhub.planetlink.model.request

import kotlin.js.JsExport

@JsExport
class LinkForm(
    /** Link URL */
    var uri: String,
    /** Link card title */
    var title: String,
    /** Link card description */
    var description: String,
) {
    /** Optional link card thumbnail */
    var thumbnail: MediaForm? = null

    /** Copy this object */
    fun copy(): LinkForm {
        return LinkForm(uri, title, description).also {
            it.thumbnail = thumbnail?.copy()
        }
    }
}
