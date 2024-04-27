package work.socialhub.planetlink.tumblr.model

import work.socialhub.planetlink.define.ReactionType
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service

/**
 * Tumblr Comment Model
 * Tumblr のコメント情報
 */
class TumblrComment(
    service: Service
) : Comment(service) {

    /** Note (Reaction) count  */
    var noteCount: Int? = null

    /** User already liked this post  */
    var liked: Boolean = false

    /** Reblog key  */
    var reblogKey: String? = null

    override val displayComment: Comment
        get() = sharedComment ?: this

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