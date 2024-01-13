package work.socialhub.planetlink.action.request

import work.socialhub.planetlink.action.RequestActionImpl.SerializedRequest
import work.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.NotSupportedException

class UsersRequestImpl : UsersRequest {

    var usersFunction: ((Paging) -> Pageable<User>)? = null

    /**
     * {@inheritDoc}
     */
    override var actionType: ActionType? = null

    /**
     * {@inheritDoc}
     */
    override var account: Account? = null

    /**
     * {@inheritDoc}
     */
    override fun users(
        paging: Paging
    ): Pageable<User> {
        return usersFunction?.invoke(paging)
            ?: throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override var serializedRequest: SerializedRequest? = null
}
