package work.socialhub.planetlink.model

import kotlinx.serialization.encodeToString
import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.utils.SerializeUtil

/**
 * Common Request Interface
 * リクエスト汎用インターフェース
 */
interface Request {

    /**
     * Get Action Type
     * アクションタイプを取得
     */
    val actionType: ActionType?

    /**
     * Get Account Info
     * アカウントを取得
     */
    val account: Account?

    /**
     * To Serialized String
     * シリアライズ化された文字列を取得
     */
    fun toRawString(): String? {
        return raw?.let {
            SerializeUtil.json.encodeToString(it)
        }
    }

    /**
     * Set serialized request
     * (復元された場合) 復元元データ
     */
    var raw: SerializedRequest?
}
