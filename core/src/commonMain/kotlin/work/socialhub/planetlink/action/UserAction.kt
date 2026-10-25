package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.User
import kotlin.js.JsExport

@JsExport
interface UserAction {

    /**
     * Get Account
     * アカウントを再度取得
     */
    @Throws(Throwable::class)
    suspend fun userRefresh(): User

    /**
     * Follow User
     * アカウントをフォロー
     */
    @Throws(Throwable::class)
    suspend fun follow()

    /**
     * UnFollow User
     * アカウントをアンフォロー
     */
    @Throws(Throwable::class)
    suspend fun unfollow()

    /**
     * Mute User
     * ユーザーをミュート
     */
    @Throws(Throwable::class)
    suspend fun mute()

    /**
     * UnMute User
     * ユーザーをミュート解除
     */
    @Throws(Throwable::class)
    suspend fun unmute()

    /**
     * Block User
     * ユーザーをブロック
     */
    @Throws(Throwable::class)
    suspend fun block()

    /**
     * UnBlock User
     * ユーザーをブロック解除
     */
    @Throws(Throwable::class)
    suspend fun unblock()

    /**
     * Get relationship
     * 認証アカウントとの関係を取得
     */
    @Throws(Throwable::class)
    suspend fun relationship(): Relationship?
}
