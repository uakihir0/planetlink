package net.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.CommentsRequestGroupAction

class CommentsRequestGroupImpl(vararg requests: CommentsRequest?) : CommentsRequestGroup {
    /** List of Request Actions  */
    override var requests: MutableList<CommentsRequest> = java.util.ArrayList<CommentsRequest>()

    init {
        addCommentsRequests(*requests)
    }

    /**
     * {@inheritDoc}
     */
    override fun addCommentsRequests(request: CommentsRequest?) {
        if (request != null) {
            requests.add(request)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun addCommentsRequests(vararg requests: CommentsRequest?) {
        if (requests != null && requests.size > 0) {
            this.requests.addAll(java.util.Arrays.asList<Array<CommentsRequest>>(*requests))
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun action(): CommentsRequestGroupAction {
        return CommentsRequestGroupActionImpl(this)
    }

    //region // Getter&Setter
    fun getRequests(): List<CommentsRequest> {
        return requests
    }

    fun setRequests(requests: MutableList<CommentsRequest>) {
        this.requests = requests
    } //endregion
}
