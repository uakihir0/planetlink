package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.action.ActionType

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
}
