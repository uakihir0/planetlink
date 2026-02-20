package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.User

interface UserAction {

    /**
     * Get Account
     * アカウントを再度取得
     */
    suspend fun userRefresh(): User

    /**
     * Follow User
     * アカウントをフォロー
     */
    suspend fun follow()

    /**
     * UnFollow User
     * アカウントをアンフォロー
     */
    suspend fun unfollow()

    /**
     * Mute User
     * ユーザーをミュート
     */
    suspend fun mute()

    /**
     * UnMute User
     * ユーザーをミュート解除
     */
    suspend fun unmute()

    /**
     * Block User
     * ユーザーをブロック
     */
    suspend fun block()

    /**
     * UnBlock User
     * ユーザーをブロック解除
     */
    suspend fun unblock()

    /**
     * Get relationship
     * 認証アカウントとの関係を取得
     */
    suspend fun relationship(): Relationship?
}
