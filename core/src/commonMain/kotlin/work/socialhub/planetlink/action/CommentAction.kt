package net.socialhub.planetlink.action

import work.socialhub.planetlink.model.Context
import work.socialhub.planetlink.model.Comment

interface CommentAction {
    /**
     * Get Comment
     * コメントを再度取得
     */
    fun refresh(): Comment?

    /**
     * Add Reaction
     * リアクションをする
     */
    fun reaction(action: String?)

    /**
     * Remove Reaction
     * リアクションをする
     */
    fun unreaction(action: String?)

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
     * Get Comment Context
     * 前後のコメントを取得する
     */
    val context: Context?

    // ============================================================== //
    // Alias
    // エイリアス
    // ============================================================== //
    /** Like <-> Favorite  */
    fun favorite() {
        like()
    }

    fun unfavorite() {
        unlike()
    }

    /** Share <-> Retweet  */
    fun retweet() {
        share()
    }

    fun unretweet() {
        unshare()
    }
}
