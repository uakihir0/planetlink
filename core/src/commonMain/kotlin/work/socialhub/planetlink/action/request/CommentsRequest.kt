package net.socialhub.planetlink.action.request

import net.socialhub.planetlink.action.RequestActionImpl.SerializeBuilder
import net.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import net.socialhub.planetlink.model.Request
import net.socialhub.planetlink.model.Stream
import net.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.Comment

interface CommentsRequest : Request {
    /**
     * Get Comments
     * コメントを取得
     */
    fun getComments(paging: Paging?): Pageable<Comment?>?

    /**
     * Get Comment Stream
     * コメントストリームを取得
     */
    fun setCommentsStream(callback: EventCallback?): Stream?

    /**
     * Get Flags of Comment Stream Support
     * コメントストリームが使用可能かを？
     */
    fun canUseCommentsStream(): Boolean

    /**
     * Make Comment Request
     * コメントリクエストの雛形作成
     */
    val commentFrom: CommentForm?

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
