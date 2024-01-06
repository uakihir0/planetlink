package work.socialhub.planetlink.action.group

import net.socialhub.planetlink.model.group.CommentGroup

interface CommentGroupAction {

    /**
     * Get Newer Comments
     * 最新コメントを取得
     */
    fun newComments(): CommentGroup

    /**
     * Get Older Comments
     * 遡ってコメントを取得
     */
    fun pastComments(): CommentGroup
}
