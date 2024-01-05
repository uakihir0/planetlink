package work.socialhub.planetlink.action.callback.comment

import net.socialhub.planetlink.action.callback.EventCallback
import net.socialhub.planetlink.model.event.IdentifyEvent

interface DeleteCommentCallback : EventCallback {
    fun onDelete(event: IdentifyEvent?)
}
