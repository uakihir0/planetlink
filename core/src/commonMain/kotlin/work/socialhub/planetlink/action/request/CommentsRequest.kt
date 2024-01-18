package work.socialhub.planetlink.action.request

import kotlinx.serialization.encodeToString
import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.utils.SerializeUtil

interface CommentsRequest : Request {

    /**
     * Get Comments
     * コメントを取得
     */
    fun comments(paging: Paging): Pageable<Comment>

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
