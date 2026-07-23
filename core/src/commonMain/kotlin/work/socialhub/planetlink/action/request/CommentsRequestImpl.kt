package work.socialhub.planetlink.action.request

import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.request.CommentForm
import kotlin.js.JsExport

@JsExport
class CommentsRequestImpl : CommentsRequest {

    var commentsFunction: (suspend (Paging) -> Pageable<Comment>)? = null
    var streamFunction: (suspend (EventCallback) -> Stream)? = null
    var updateStreamFunction: (suspend (List<Comment>, EventCallback) -> CommentUpdateStream)? = null
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
    override suspend fun setCommentsStream(
        callback: EventCallback
    ): Stream {
        return streamFunction?.invoke(callback)
            ?: throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun setCommentsUpdateStream(
        comments: List<Comment>,
        callback: EventCallback,
    ): CommentUpdateStream {
        return updateStreamFunction?.invoke(comments, callback)
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
    override fun canUseCommentsUpdateStream(): Boolean {
        return updateStreamFunction != null
    }

    /**
     * {@inheritDoc}
     */
    override fun commentFrom(): CommentForm {
        return commentForm ?: CommentForm()
            .also { this.commentForm = it }
    }
}
