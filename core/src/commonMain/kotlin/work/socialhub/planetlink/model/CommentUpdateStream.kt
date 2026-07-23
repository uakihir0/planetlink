package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * Stream of updates for comments that have already been fetched.
 */
@JsExport
interface CommentUpdateStream : Stream {

    /**
     * Add comments to the update subscription.
     *
     * Calling this before [open] queues the comments for subscription.
     */
    suspend fun addComments(comments: List<Comment>)

    /**
     * Remove comments from the update subscription.
     */
    suspend fun removeComments(comments: List<Comment>)
}
