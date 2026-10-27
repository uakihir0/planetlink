package work.socialhub.planetlink.tumblr.model

import work.socialhub.planetlink.define.ReactionType
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.tumblr.define.TumblrFormKey
import kotlin.js.JsExport

/**
 * Tumblr Comment Model
 * Tumblr のコメント情報
 */
@JsExport
class TumblrComment(
    service: Service
) : Comment(service) {

    /** Note (Reaction) count  */
    var noteCount: Int? = null

    /** User already liked this post  */
    var liked: Boolean = false

    /** Reblog key  */
    var reblogKey: String? = null

    /**
     * ID of the post that [reblogKey] belongs to, when it is not [id].
     * The shared (root) comment of a reblog has the root post id, but only
     * the reblog's key is known, so reblogging it must target the reblog.
     */
    var reblogKeyPostId: String? = null

    /** Parent post ID for context tracking */
    var parentId: String? = null

    /** Root post ID for context tracking */
    var rootId: String? = null

    override val displayComment: Comment
        get() = sharedComment ?: this

    /**
     * Quote (= reblog with comment) form.
     * Tumblr needs the reblog key in addition to the post id.
     */
    override val quoteForm: CommentForm
        get() = CommentForm()
            .quoteId(reblogKeyPostId?.let { ID(it) } ?: id)
            .isMessage(false)
            .also { form ->
                reblogKey?.let {
                    form.addParam(TumblrFormKey.REBLOG_KEY, it)
                }
            }

    override var reactions: List<Reaction> = listOf()
        get() = field.toMutableList()
            .also { list ->
                noteCount?.let {
                    Reaction().also {
                        it.count = noteCount
                        it.name = "note"
                        list.add(it)
                    }
                }
            }

    override fun applyReaction(
        reaction: Reaction
    ) {
        val name = checkNotNull(reaction.name)
        if (ReactionType.Like.codes.contains(name)) {
            if (reaction.reacting && !liked) {
                liked = true
            }
            if (!reaction.reacting && liked) {
                liked = false
            }
        }
    }
}