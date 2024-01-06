package net.socialhub.planetlink.action

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context

class CommentActionImpl(action: AccountAction) : CommentAction {
    private val action: AccountAction = action
    private var comment: Comment? = null

    fun comment(comment: Comment?): CommentAction {
        this.comment = comment
        return this
    }

    /**
     * {@inheritDoc}
     */
    override fun refresh(): Comment {
        return action.getComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override fun reaction(action: String?) {
        this.action.reactionComment(comment, action)
    }

    /**
     * {@inheritDoc}
     */
    override fun unreaction(action: String?) {
        this.action.unreactionComment(comment, action)
    }

    /**
     * {@inheritDoc}
     */
    override fun like() {
        action.likeComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override fun unlike() {
        action.unlikeComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override fun share() {
        action.shareComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override fun unshare() {
        action.unshareComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override fun delete() {
        action.deleteComment(comment)
    }

    override val context: Context?
        /**
         * {@inheritDoc}
         */
        get() = action.getCommentContext(comment)
}
