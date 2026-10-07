package work.socialhub.planetlink.mixi2.model

import kotlin.js.JsExport
import work.socialhub.planetlink.micro.MicroBlogComment
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm

/**
 * mixi2 comment (post) model.
 *
 * A mixi2 post likes with one heart and reacts with stamps; both travel as
 * [reactions], with a stamp reaction carrying the stamp id and its image URL.
 * A repost is a post that carries no text and a [sharedComment].
 */
@JsExport
class Mixi2Comment(
    service: Service
) : MicroBlogComment(service) {

    /** Number of replies to this post. */
    var replyCount: Int? = null

    /** Number of quotes of this post. */
    var quoteCount: Int? = null

    /** Whether the authenticated persona has bookmarked this post. */
    var bookmarked: Boolean = false

    /** Community the post was written to, if any. */
    var communityId: String? = null

    /** Name of the community the post was written to, if any. */
    var communityName: String? = null

    /** Chat room the message belongs to, or null on a public post. */
    var roomId: String? = null

    private var storedReactions: List<Reaction> = listOf()

    override var reactions: List<Reaction>
        get() = storedReactions + listOfNotNull(
            reaction("like", likeCount, liked),
            reaction("share", shareCount, shared),
            reaction("reply", replyCount),
            reaction("quote", quoteCount),
        )
        set(value) {
            storedReactions = value
        }

    private fun reaction(
        name: String,
        count: Int?,
        reacting: Boolean = false,
    ): Reaction? {
        return count?.takeIf { it > 0 }?.let {
            Reaction().also { reaction ->
                reaction.name = name
                reaction.count = it
                reaction.reacting = reacting
            }
        }
    }

    override val displayComment: Comment
        get() = if (isOnlyShared) checkNotNull(sharedComment) else this

    override val replyForm: CommentForm
        get() = CommentForm().also {
            val room = roomId
            it.replyId(if (directMessage && room != null) ID(room) else id)
            it.isMessage(directMessage)
        }

    override val quoteForm: CommentForm
        get() = CommentForm().also {
            it.quoteId(id)
        }

    override var webUrl: String = ""
        get() = field.ifEmpty {
            val host = service.host ?: "https://mixi.social"
            val value = id<String>()
            val handle = (user as? Mixi2User)?.handle
            if (handle.isNullOrBlank()) {
                "$host/posts/$value"
            } else {
                "$host/@$handle/posts/$value"
            }.also { field = it }
        }
}
