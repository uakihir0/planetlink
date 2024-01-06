package work.socialhub.planetlink.action.callback.comment

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.event.IdentifyEvent

interface DeleteCommentCallback : EventCallback {
    fun onDelete(event: IdentifyEvent?)
}
