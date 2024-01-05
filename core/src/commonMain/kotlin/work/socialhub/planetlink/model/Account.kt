package work.socialhub.planetlink.model

import net.socialhub.planetlink.action.AccountAction

/**
 * Account Model
 * (Not SNS User model)
 * アカウント情報を扱うモデル
 * (各サービス毎のユーザーではない点に注意)
 */
class Account {

    var tag: String? = null
    lateinit var service: Service
    lateinit var action: AccountAction
}

