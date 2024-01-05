package net.socialhub.planetlink.action.group

import net.socialhub.planetlink.model.group.CommentGroup

interface CommentsRequestGroupAction {
    /**
     * Get Comments.
     * default count is 200.
     */
    val comments: CommentGroup?

    /**
     * Get Comments with count.
     */
    fun getComments(count: Int): CommentGroup?
}
