package work.socialhub.planetlink.action.request

import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.request.CommentForm

class CommentsRequestImpl : CommentsRequest {

    var commentsFunction: (suspend (Paging) -> Pageable<Comment>)? = null
    var streamFunction: ((EventCallback) -> Stream)? = null
    var commentForm: CommentForm? = null
    var streamRecommended = true

    /**
     * {@inheritDoc}
     */
    override var actionType: ActionType? = null

    /**
     * {@inheritDoc}
     */
    override var account: Account? = null

    /**
     * {@inheritDoc}
     */
    override var raw: SerializedRequest? = null

    /**
     * {@inheritDoc}
     */
    override suspend fun comments(
        paging: Paging
    ): Pageable<Comment> {
        return commentsFunction?.invoke(paging)
            ?: throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override fun setCommentsStream(
        callback: EventCallback
    ): Stream {
        return streamFunction?.invoke(callback)
            ?: throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override fun canUseCommentsStream(): Boolean {
        return streamRecommended && (streamFunction != null)
    }

    /**
     * {@inheritDoc}
     */
    override fun commentFrom(): CommentForm {
        return commentForm ?: CommentForm()
            .also { this.commentForm = it }
    }
}
