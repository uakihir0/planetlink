package work.socialhub.planetlink.action.request

import net.socialhub.planetlink.action.RequestActionImpl.SerializeBuilder
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Stream
import net.socialhub.planetlink.model.error.NotSupportedException
import net.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment

class CommentsRequestImpl : CommentsRequest {

    private var commentsFunction: java.util.function.Function<Paging, Pageable<Comment>>? = null
    private var streamFunction: java.util.function.Function<EventCallback, Stream>? = null

    /**
     * {@inheritDoc}
     */
    override var serializeBuilder: SerializeBuilder? = null
    private var commentForm: CommentForm? = null

    private var streamRecommended = true

    /**
     * {@inheritDoc}
     */
    override var actionType: ActionType? = null

    /**
     * {@inheritDoc}
     *///region // Getter&Setter
    override var account: Account? = null

    /**
     * {@inheritDoc}
     */
    override fun getComments(paging: Paging?): Pageable<Comment> {
        if (commentsFunction == null) {
            throw NotSupportedException()
        }
        return commentsFunction.apply(paging)
    }

    /**
     * {@inheritDoc}
     */
    override fun setCommentsStream(callback: EventCallback?): Stream {
        if (streamFunction == null) {
            throw NotSupportedException()
        }
        return streamFunction.apply(callback)
    }

    /**
     * {@inheritDoc}
     */
    override fun canUseCommentsStream(): Boolean {
        return streamRecommended && (streamFunction != null)
    }

    override val commentFrom: CommentForm?
        /**
         * {@inheritDoc}
         */
        get() {
            if (commentForm == null) {
                commentForm = CommentForm()
            }
            return commentForm
        }

    /**
     * {@inheritDoc}
     */
    override fun toSerializedString(): String {
        return serializeBuilder!!.toJson()
    }

    fun setCommentsFunction(commentsFunction: java.util.function.Function<Paging?, Pageable<Comment?>?>) {
        this.commentsFunction = commentsFunction
    }

    fun setStreamFunction(streamFunction: java.util.function.Function<EventCallback?, Stream?>) {
        this.streamFunction = streamFunction
    }

    fun setCommentForm(commentForm: CommentForm?) {
        this.commentForm = commentForm
    }

    fun setStreamRecommended(streamRecommended: Boolean) {
        this.streamRecommended = streamRecommended
    } //endregion
}
