package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context

interface CommentAction {

    /**
     * Get Comment
     * コメントを再度取得
     */
    fun commentRefresh(): Comment

    /**
     * Add Reaction
     * リアクションをする
     */
    fun reaction(action: String)

    /**
     * Remove Reaction
     * リアクションをする
     */
    fun unreaction(action: String)

    /**
     * Like Comment
     * コメントをいいねする
     */
    fun like()

    /**
     * UnLike Comment
     * コメントのいいねを消す
     */
    fun unlike()

    /**
     * Share Comment
     * コメントをシェアする
     */
    fun share()

    /**
     * Unshare Comment
     * コメントのシェアを取り消す
     */
    fun unshare()

    /**
     * Delete Comment
     * 自分のコメントを削除
     */
    fun delete()

    /**
     * Get Comment Contexts
     * 前後のコメントを取得する
     */
    fun commentContexts(): Context
}
