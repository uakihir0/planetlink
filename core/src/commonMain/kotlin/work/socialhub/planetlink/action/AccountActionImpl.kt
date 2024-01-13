package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.User

abstract class AccountActionImpl(
    var account: Account
) : AccountAction {

    @Suppress("UNCHECKED_CAST")
    fun <T : AccountActionImpl> account(
        account: Account
    ): T {
        this.account = account
        return this as T
    }

    /**
     * {@inheritDoc}
     */
    override fun request(): RequestAction {
        return RequestActionImpl(account)
    }

    /**
     * Cached User me.
     * キャッシュユーザー情報
     */
    private var me: User? = null

    /**
     * Get User me with cache.
     * キャッシュ付きで自分のユーザーを取得
     */
    fun userMeWithCache(): User {
        return me ?: run {
            userMe().also { me = it }
        }
    }
}
