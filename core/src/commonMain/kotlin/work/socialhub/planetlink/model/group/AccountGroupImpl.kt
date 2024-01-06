package net.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.AccountGroupAction

class AccountGroupImpl(vararg accounts: Account?) : AccountGroup {
    override var accounts: MutableList<Account> = java.util.ArrayList<Account>()

    /**
     * コンストラクタ
     */
    init {
        if (accounts != null && accounts.size > 0) {
            this.accounts.addAll(java.util.Arrays.asList<Account>(*accounts))
        }
    }

    override fun addAccount(account: Account) {
        accounts.add(account)
    }

    override fun action(): AccountGroupAction {
        return AccountGroupActionImpl(this)
    }

    //region // Getter&Setter
    fun getAccounts(): List<Account> {
        return accounts
    }

    fun setAccounts(accounts: MutableList<Account>) {
        this.accounts = accounts
    } //endregion
}
