package work.socialhub.planetlink.action.callback.comment

import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.action.callback.EventCallback

interface MentionCommentCallback : EventCallback {
    fun onMention(event: CommentEvent?)
}
