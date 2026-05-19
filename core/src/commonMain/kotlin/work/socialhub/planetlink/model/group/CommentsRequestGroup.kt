package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.CommentsRequestGroupAction
import work.socialhub.planetlink.action.request.CommentsRequest
import kotlin.js.JsExport

@JsExport
interface CommentsRequestGroup {

    val requests: MutableList<CommentsRequest>

    /**
     * Add Comments Request
     */
    @JsExport.Ignore
    fun addCommentsRequests(request: CommentsRequest)

    /**
     * Add Comments Requests
     */
    fun addCommentsRequests(vararg requests: CommentsRequest)

    /**
     * Get Action
     */
    fun action(): CommentsRequestGroupAction

    companion object {
        @JsExport.Ignore
        fun of(): CommentsRequestGroup {
            return CommentsRequestGroupImpl()
        }

        fun of(vararg requests: CommentsRequest): CommentsRequestGroup {
            return CommentsRequestGroupImpl(*requests)
        }
    }
}
