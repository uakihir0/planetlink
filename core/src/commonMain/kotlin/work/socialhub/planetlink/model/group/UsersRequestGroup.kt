package net.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.UsersRequestGroupAction

interface UsersRequestGroup {
    /**
     * Add Users Request
     */
    fun addUsersRequests(request: UsersRequest?)

    /**
     * Add Users Requests
     */
    fun addUsersRequests(vararg requests: UsersRequest?)

    @get:Nonnull
    val requests: List<Any?>?

    /**
     * Get Action
     */
    @Nonnull
    fun action(): UsersRequestGroupAction?

    companion object {
        fun of(): UsersRequestGroup? {
            return UsersRequestGroupImpl()
        }

        fun of(vararg requests: UsersRequest?): UsersRequestGroup? {
            return UsersRequestGroupImpl(requests)
        }
    }
}
