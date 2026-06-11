package work.socialhub.planetlink.action.callback.comment

import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.action.callback.EventCallback
import kotlin.js.JsExport

@JsExport
interface UpdateCommentCallback : EventCallback {
    // See EventCallback.kt for why companion object is needed
    companion object
    fun onUpdate(event: CommentEvent?)
}
