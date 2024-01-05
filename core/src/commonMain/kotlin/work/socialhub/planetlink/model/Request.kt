package net.socialhub.planetlink.model

import net.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.model.Account

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
