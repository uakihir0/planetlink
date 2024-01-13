package work.socialhub.planetlink.model.group

import kotlinx.datetime.Instant
import work.socialhub.planetlink.action.group.CommentGroupAction
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Pageable

interface CommentGroup {

    /**
     * Get Comments Request Groups
     * コメントグループの取得
     */
    val requestGroup: CommentsRequestGroup

    /**
     * Return Comments related to Comments Requests
     * コメントリクエスト毎に紐づくコメントを取得
     */
    val entities: Map<CommentsRequest, Pageable<Comment>>

    /**
     * Return Order Decided Comments
     * (Return Comments by Created Date DESC)
     * 順序が決定している部分までコメントを取得
     */
    fun comments(): Pageable<Comment>

    /**
     * Get MaxDate for Paging Request
     */
    val maxDate: Instant?

    /**
     * Get SinceDate for Paging Request
     */
    val sinceDate: Instant?

    /**
     * Set Newest comment. (for streaming)
     */
    fun setNewestComment(comment: Comment)

    /**
     * Set Oldest comment.
     */
    fun setOldestComment(comment: Comment)

    /**
     * Get Actions
     */
    fun action(): CommentGroupAction?
}
