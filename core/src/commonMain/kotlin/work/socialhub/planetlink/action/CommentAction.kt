package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context
import kotlin.js.JsExport

@JsExport
interface CommentAction {

    /**
     * Get Comment
     * コメントを再度取得
     */
    @Throws(Throwable::class)
    suspend fun commentRefresh(): Comment

    /**
     * Add Reaction
     * リアクションをする
     */
    @Throws(Throwable::class)
    suspend fun reaction(action: String)

    /**
     * Remove Reaction
     * リアクションをする
     */
    @Throws(Throwable::class)
    suspend fun unreaction(action: String)

    /**
     * Like Comment
     * コメントをいいねする
     */
    @Throws(Throwable::class)
    suspend fun like()

    /**
     * UnLike Comment
     * コメントのいいねを消す
     */
    @Throws(Throwable::class)
    suspend fun unlike()

    /**
     * Share Comment
     * コメントをシェアする
     */
    @Throws(Throwable::class)
    suspend fun share()

    /**
     * Unshare Comment
     * コメントのシェアを取り消す
     */
    @Throws(Throwable::class)
    suspend fun unshare()

    /**
     * Delete Comment
     * 自分のコメントを削除
     */
    @Throws(Throwable::class)
    suspend fun delete()

    /**
     * Get Comment Contexts
     * 前後のコメントを取得する
     */
    @Throws(Throwable::class)
    suspend fun commentContexts(): Context
}
