package net.socialhub.planetlink.action.request

import net.socialhub.planetlink.action.RequestActionImpl.SerializeBuilder
import work.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.Account

class UsersRequestImpl : UsersRequest {
    private var usersFunction: java.util.function.Function<Paging, Pageable<User>>? = null

    /**
     * {@inheritDoc}
     */
    override var serializeBuilder: SerializeBuilder? = null

    /**
     * {@inheritDoc}
     *///endregion
    override var actionType: ActionType? = null

    /**
     * {@inheritDoc}
     *///region // Getter&Setter
    override var account: Account? = null

    /**
     * {@inheritDoc}
     */
    override fun getUsers(paging: Paging?): Pageable<User> {
        return usersFunction.apply(paging)
    }

    /**
     * {@inheritDoc}
     */
    override fun toSerializedString(): String {
        return serializeBuilder!!.toJson()
    }

    fun setUsersFunction(usersFunction: java.util.function.Function<Paging?, Pageable<User?>?>) {
        this.usersFunction = usersFunction
    }
}
