package work.socialhub.planetlink.action.request

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.request.CommentForm
import kotlin.js.JsExport

@JsExport
interface CommentsRequest : Request {

    /**
     * Get Comments
     * コメントを取得
     */
    @Throws(Throwable::class)
    suspend fun comments(paging: Paging): Pageable<Comment>

    /**
     * Set Comment Stream
     * コメントストリームを設定
     */
    @Throws(Throwable::class)
    suspend fun setCommentsStream(callback: EventCallback): Stream

    /**
     * Set a stream for updates to comments returned by this request.
     */
    @Throws(Throwable::class)
    suspend fun setCommentsUpdateStream(
        comments: List<Comment>,
        callback: EventCallback,
    ): CommentUpdateStream

    /**
     * Get Flags of Comment Stream Support
     * コメントストリームが使用可能かを？
     */
    @Throws(Throwable::class)
    fun canUseCommentsStream(): Boolean

    /**
     * Get whether updates to fetched comments can be streamed.
     */
    @Throws(Throwable::class)
    fun canUseCommentsUpdateStream(): Boolean

    /**
     * Make Comment Request
     * コメントリクエストの雛形作成
     */
    @Throws(Throwable::class)
    fun commentFrom(): CommentForm
}
