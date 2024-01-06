package net.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.CommentGroupAction
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Comment

interface CommentGroup {
    /**
     * Get Comments Request Groups
     * コメントグループの取得
     */
    val requestGroup: net.socialhub.planetlink.model.group.CommentsRequestGroup?

    /**
     * Return Comments related to Comments Requests
     * コメントリクエスト毎に紐づくコメントを取得
     */
    val entities: Map<Any?, Pageable<Comment?>?>?

    /**
     * Return Order Decided Comments
     * (Return Comments by Created Date DESC)
     * 順序が決定している部分までコメントを取得
     */
    val comments: Pageable<Comment?>?

    /**
     * Get MaxDate for Paging Request
     */
    val maxDate: java.util.Date?

    /**
     * Get SinceDate for Paging Request
     */
    val sinceDate: java.util.Date?

    /**
     * Set Newest comment. (for streaming)
     */
    fun setNewestComment(comment: Comment?)

    /**
     * Set Oldest comment.
     */
    fun setOldestComment(comment: Comment?)


    /**
     * Get Actions
     */
    fun action(): CommentGroupAction?
}
