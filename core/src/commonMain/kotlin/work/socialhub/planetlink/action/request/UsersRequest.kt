package net.socialhub.planetlink.action.request

import net.socialhub.planetlink.action.RequestActionImpl.SerializeBuilder
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Request
import work.socialhub.planetlink.model.User

interface UsersRequest : Request {
    /**
     * Get Users
     * ユーザーを取得
     */
    fun getUsers(paging: Paging?): Pageable<User?>?

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
