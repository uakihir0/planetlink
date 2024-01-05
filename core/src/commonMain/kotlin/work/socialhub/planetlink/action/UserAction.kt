package net.socialhub.planetlink.action

import net.socialhub.planetlink.model.Relationship
import net.socialhub.planetlink.model.User

interface UserAction {
    /**
     * Get Account
     * アカウントを再度取得
     */
    fun refresh(): User?

    /**
     * Follow User
     * アカウントをフォロー
     */
    fun follow()

    /**
     * UnFollow User
     * アカウントをアンフォロー
     */
    fun unfollow()

    /**
     * Mute User
     * ユーザーをミュート
     */
    fun mute()

    /**
     * UnMute User
     * ユーザーをミュート解除
     */
    fun unmute()

    /**
     * Block User
     * ユーザーをブロック
     */
    fun block()

    /**
     * UnBlock User
     * ユーザーをブロック解除
     */
    fun unblock()

    /**
     * Get relationship
     * 認証アカウントとの関係を取得
     */
    val relationship: Relationship?
}
