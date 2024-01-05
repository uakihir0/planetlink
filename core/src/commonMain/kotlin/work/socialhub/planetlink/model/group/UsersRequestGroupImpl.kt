package net.socialhub.planetlink.model.group

import net.socialhub.planetlink.action.group.UsersRequestGroupAction

class UsersRequestGroupImpl(vararg requests: UsersRequest?) : UsersRequestGroup {
    /** List of Request Actions  */
    override var requests: MutableList<UsersRequest> = java.util.ArrayList<UsersRequest>()

    init {
        addUsersRequests(*requests)
    }

    /**
     * {@inheritDoc}
     */
    override fun addUsersRequests(request: UsersRequest?) {
        if (request != null) {
            requests.add(request)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun addUsersRequests(vararg requests: UsersRequest?) {
        if (requests != null && requests.size > 0) {
            this.requests.addAll(java.util.Arrays.asList<Array<UsersRequest>>(*requests))
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun action(): UsersRequestGroupAction {
        return UsersRequestGroupActionImpl(this)
    }

    //region // Getter&Setter
    fun getRequests(): List<UsersRequest> {
        return requests
    }

    fun setRequests(requests: MutableList<UsersRequest>) {
        this.requests = requests
    } //endregion
}
