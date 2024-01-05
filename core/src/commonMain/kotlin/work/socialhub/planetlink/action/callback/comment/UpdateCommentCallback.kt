package net.socialhub.planetlink.action.callback.comment

import net.socialhub.planetlink.action.callback.EventCallback
import net.socialhub.planetlink.model.event.CommentEvent

interface UpdateCommentCallback : EventCallback {
    fun onUpdate(event: CommentEvent?)
}
