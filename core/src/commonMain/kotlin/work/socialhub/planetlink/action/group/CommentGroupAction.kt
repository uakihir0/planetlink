package work.socialhub.planetlink.action.group

import work.socialhub.planetlink.model.group.CommentGroup

interface CommentGroupAction {

    /**
     * Get Newer Comments
     * 最新コメントを取得
     */
    suspend fun newComments(): CommentGroup

    /**
     * Get Older Comments
     * 遡ってコメントを取得
     */
    suspend fun pastComments(): CommentGroup
}
