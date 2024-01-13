package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.UsersRequestGroupAction
import work.socialhub.planetlink.action.group.UsersRequestGroupActionImpl
import work.socialhub.planetlink.action.request.UsersRequest

class UsersRequestGroupImpl(
    vararg requests: UsersRequest
) : UsersRequestGroup {

    /** List of Request Actions  */
    override var requests = mutableListOf<UsersRequest>()

    init {
        addUsersRequests(*requests)
    }

    /**
     * {@inheritDoc}
     */
    override fun addUsersRequests(request: UsersRequest) {
        requests.add(request)
    }

    /**
     * {@inheritDoc}
     */
    override fun addUsersRequests(vararg requests: UsersRequest) {
        if (requests.isNotEmpty()) {
            this.requests.addAll(requests)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun action(): UsersRequestGroupAction {
        return UsersRequestGroupActionImpl(this)
    }
}
