package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.CommentsRequestGroupAction
import work.socialhub.planetlink.action.group.CommentsRequestGroupActionImpl
import work.socialhub.planetlink.action.request.CommentsRequest

class CommentsRequestGroupImpl(
    vararg requests: CommentsRequest
) : CommentsRequestGroup {

    /** List of Request Actions  */
    override var requests = mutableListOf<CommentsRequest>()

    init {
        addCommentsRequests(*requests)
    }

    /**
     * {@inheritDoc}
     */
    override fun addCommentsRequests(request: CommentsRequest) {
        this.requests.add(request)
    }

    /**
     * {@inheritDoc}
     */
    override fun addCommentsRequests(vararg requests: CommentsRequest) {
        if (requests.isNotEmpty()) {
            this.requests.addAll(requests)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun action(): CommentsRequestGroupAction {
        return CommentsRequestGroupActionImpl(this)
    }
}
