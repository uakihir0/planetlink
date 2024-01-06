package work.socialhub.planetlink.action.request

import net.socialhub.planetlink.action.RequestActionImpl.SerializeBuilder
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Request
import work.socialhub.planetlink.model.Stream
import net.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.Comment

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

    /**
     * To Serialized String
     * シリアライズ化された文字列を取得
     */
    fun toSerializedString(): String?

    /**
     * Get Serialize Builder
     * シリアライズビルダーの取得
     */
    val serializeBuilder: SerializeBuilder?
}
