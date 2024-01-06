package work.socialhub.planetlink.action.callback.comment

import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.action.callback.EventCallback

interface ShareCommentCallback : EventCallback {
    fun onShare(event: CommentEvent?)
}
