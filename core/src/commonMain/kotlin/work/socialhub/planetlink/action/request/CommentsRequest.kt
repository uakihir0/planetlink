package work.socialhub.planetlink.action.request

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.request.CommentForm

interface CommentsRequest : Request {

    /**
     * Get Comments
     * コメントを取得
     */
    suspend fun comments(paging: Paging): Pageable<Comment>

    /**
     * Set Comment Stream
     * コメントストリームを設定
     */
    fun setCommentsStream(callback: EventCallback): Stream

    /**
     * Get Flags of Comment Stream Support
     * コメントストリームが使用可能かを？
     */
    fun canUseCommentsStream(): Boolean

    /**
     * Make Comment Request
     * コメントリクエストの雛形作成
     */
    fun commentFrom(): CommentForm
}
