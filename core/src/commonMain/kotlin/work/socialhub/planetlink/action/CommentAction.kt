package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context

interface CommentAction {

    /**
     * Get Comment
     * コメントを再度取得
     */
    suspend fun commentRefresh(): Comment

    /**
     * Add Reaction
     * リアクションをする
     */
    suspend fun reaction(action: String)

    /**
     * Remove Reaction
     * リアクションをする
     */
    suspend fun unreaction(action: String)

    /**
     * Like Comment
     * コメントをいいねする
     */
    suspend fun like()

    /**
     * UnLike Comment
     * コメントのいいねを消す
     */
    suspend fun unlike()

    /**
     * Share Comment
     * コメントをシェアする
     */
    suspend fun share()

    /**
     * Unshare Comment
     * コメントのシェアを取り消す
     */
    suspend fun unshare()

    /**
     * Delete Comment
     * 自分のコメントを削除
     */
    suspend fun delete()

    /**
     * Get Comment Contexts
     * 前後のコメントを取得する
     */
    suspend fun commentContexts(): Context
}
