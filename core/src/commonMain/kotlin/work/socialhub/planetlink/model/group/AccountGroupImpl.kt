package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.AccountGroupAction
import work.socialhub.planetlink.action.group.AccountGroupActionImpl
import work.socialhub.planetlink.model.Account

class AccountGroupImpl(
    vararg accounts: Account
) : AccountGroup {

    override var accounts = mutableListOf<Account>()

    /**
     * コンストラクタ
     */
    init {
        if (accounts.isNotEmpty()) {
            this.accounts.addAll(accounts)
        }
    }

    override fun addAccount(account: Account) {
        accounts.add(account)
    }

    override fun action(): AccountGroupAction {
        return AccountGroupActionImpl(this)
    }
}
