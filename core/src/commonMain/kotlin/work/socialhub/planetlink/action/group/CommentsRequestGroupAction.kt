package work.socialhub.planetlink.action.group

import net.socialhub.planetlink.model.group.CommentGroup

interface CommentsRequestGroupAction {

    /**
     * Get Comments.
     * default count is 200.
     */
    fun comments(count: Int = 200): CommentGroup
}
