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
    override suspend fun commentRefresh(): Comment {
        return action.comment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reaction(action: String) {
        this.action.reactionComment(comment, action)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unreaction(action: String) {
        this.action.unreactionComment(comment, action)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun like() {
        action.likeComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unlike() {
        action.unlikeComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun share() {
        action.shareComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unshare() {
        action.unshareComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun delete() {
        action.deleteComment(comment)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun commentContexts(): Context {
        return action.commentContexts(comment)
    }
}
