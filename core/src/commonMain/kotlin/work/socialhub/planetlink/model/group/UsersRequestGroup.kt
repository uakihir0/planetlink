package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.UsersRequestGroupAction
import work.socialhub.planetlink.action.request.UsersRequest

interface UsersRequestGroup {

    val requests: MutableList<UsersRequest>

    /**
     * Add Users Request
     */
    fun addUsersRequests(request: UsersRequest)

    /**
     * Add Users Requests
     */
    fun addUsersRequests(vararg requests: UsersRequest)

    /**
     * Get Action
     */
    fun action(): UsersRequestGroupAction

    companion object {
        fun of(): UsersRequestGroup {
            return UsersRequestGroupImpl()
        }

        fun of(vararg requests: UsersRequest): UsersRequestGroup {
            return UsersRequestGroupImpl(*requests)
        }
    }
}
