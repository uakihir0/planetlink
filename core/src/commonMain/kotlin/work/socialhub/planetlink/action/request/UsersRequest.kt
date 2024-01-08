package work.socialhub.planetlink.action.request

import kotlinx.serialization.encodeToString
import work.socialhub.planetlink.action.RequestActionImpl.SerializedRequest
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Request
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.utils.SerializeUtil

interface UsersRequest : Request {

    /**
     * Get Users
     * ユーザーを取得
     */
    fun users(paging: Paging): Pageable<User>

    /**
     * To Serialized String
     * シリアライズ化された文字列を取得
     */
    fun toSerializedString(): String? {
        return serializedRequest?.let {
            SerializeUtil.json.encodeToString(it)
        }
    }

    /**
     * Get Serialized Request
     * シリアライズリクエストの取得
     */
    val serializedRequest: SerializedRequest?
}
