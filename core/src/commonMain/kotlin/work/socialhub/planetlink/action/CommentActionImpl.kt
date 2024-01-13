package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context

class CommentActionImpl(
    var action: AccountAction,
    var comment: Comment,
) : CommentAction {

    /**
     * {@inheritDoc}
     */
    override fun commentRefresh(): Comment {
        return action.comment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override fun reaction(action: String) {
        this.action.reactionComment(comment, action)
    }

    /**
     * {@inheritDoc}
     */
    override fun unreaction(action: String) {
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

    /**
     * {@inheritDoc}
     */
    override fun commentContexts(): Context {
        return action.commentContexts(comment)
    }
}
