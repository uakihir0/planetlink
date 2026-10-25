package work.socialhub.planetlink.model

import work.socialhub.planetlink.action.AccountAction
import kotlin.js.JsExport

/**
 * Account Model
 * (Not SNS User model)
 * アカウント情報を扱うモデル
 * (各サービス毎のユーザーではない点に注意)
 */
@JsExport
class Account {

    var tag: String? = null

    // Swift から未初期化の lateinit を読むとクラッシュするため、
    // 安全な既定値を持たせる。既定の Action は、呼ぶと
    // NotImplementedException を投げるだけの実装。
    var service: Service = Service("", this)
    var action: AccountAction = UninitializedAccountAction
}

/**
 * まだアダプタが設定されていない Account の Action。
 * 呼び出すと、AccountAction の既定実装が NotImplementedException を投げる。
 */
private object UninitializedAccountAction : AccountAction

