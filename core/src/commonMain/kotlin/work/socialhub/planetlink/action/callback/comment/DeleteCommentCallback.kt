package work.socialhub.planetlink.action.callback.comment

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.event.IdentifyEvent
import kotlin.js.JsExport

@JsExport
interface DeleteCommentCallback : EventCallback {
    // See EventCallback.kt for why companion object is needed
    companion object
    fun onDelete(event: IdentifyEvent?)
}
