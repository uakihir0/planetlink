package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.AccountGroupAction
import work.socialhub.planetlink.model.Account

/**
 * Account Group Model
 * 複数のアカウントを束ねるモデル
 */
interface AccountGroup {

    /**
     * Get Accounts List
     * アカウントリストの追加
     */
    val accounts: List<Account>

    /**
     * Add Accounts
     * アカウントの追加
     */
    fun addAccount(account: Account)

    /**
     * Get Account Actions
     * アクションの取得
     */
    fun action(): AccountGroupAction

    companion object {
        fun of(vararg accounts: Account): AccountGroup {
            return AccountGroupImpl(accounts)
        }
    }
}
