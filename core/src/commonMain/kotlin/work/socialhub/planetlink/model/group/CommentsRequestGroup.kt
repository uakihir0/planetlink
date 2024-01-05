package net.socialhub.planetlink.model.group

import net.socialhub.planetlink.action.group.CommentsRequestGroupAction

interface CommentsRequestGroup {
    /**
     * Add Comments Request
     */
    fun addCommentsRequests(request: CommentsRequest?)

    /**
     * Add Comments Requests
     */
    fun addCommentsRequests(vararg requests: CommentsRequest?)

    @get:Nonnull
    val requests: List<Any?>?

    /**
     * Get Action
     */
    @Nonnull
    fun action(): CommentsRequestGroupAction?

    companion object {
        fun of(): CommentsRequestGroup? {
            return CommentsRequestGroupImpl()
        }

        fun of(vararg requests: CommentsRequest?): CommentsRequestGroup? {
            return CommentsRequestGroupImpl(requests)
        }
    }
}
