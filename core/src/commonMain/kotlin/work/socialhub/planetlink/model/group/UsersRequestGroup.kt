package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.UsersRequestGroupAction
import work.socialhub.planetlink.action.request.UsersRequest
import kotlin.js.JsExport

@JsExport
interface UsersRequestGroup {

    val requests: MutableList<UsersRequest>

    /**
     * Add Users Request
     */
    @JsExport.Ignore
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
        @JsExport.Ignore
        fun of(): UsersRequestGroup {
            return UsersRequestGroupImpl()
        }

        fun of(vararg requests: UsersRequest): UsersRequestGroup {
            return UsersRequestGroupImpl(*requests)
        }
    }
}
