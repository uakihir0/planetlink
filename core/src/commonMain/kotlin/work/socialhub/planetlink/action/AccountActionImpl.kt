package net.socialhub.planetlink.action

import net.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.User

abstract class AccountActionImpl : AccountAction {
    //endregion
    //region // Getter&Setter
    var account: Account? = null

    protected var me: User? = null

    fun <T : AccountActionImpl?> account(account: Account?): T {
        this.account = account
        return this as T
    }

    interface ActionCaller<T, E : Throwable?> {
        @Throws(E::class)
        fun proceed(): T
    }

    interface ActionRunner<E : Throwable?> {
        @Throws(E::class)
        fun proceed()
    }

    /**
     * {@inheritDoc}
     */
    override fun request(): RequestAction {
        return RequestActionImpl(account)
    }

    val userMeWithCache: User
        /**
         * Get User me with cache.
         * キャッシュ付きで自分のユーザーを取得
         */
        get() = if ((me != null)) me!! else getUserMe()
}
