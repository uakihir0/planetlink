package work.socialhub.planetlink.saypip.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service

/**
 * Saypip comment (post) model.
 *
 * A reaction in Saypip is a picture (an emoji) and nothing else, and the conversations under a
 * post are counted beside it — both arrive as [reactions].
 */
@JsExport
class SaypipComment(
    service: Service
) : Comment(service) {

    /** Whether the author is asking to be talked to. */
    var wantsTalk: Boolean = false

    /** The reply id when this comment is a message in a conversation, or null on a post. */
    var replyId: String? = null

    /** When this viewer stops being able to read the post, or null when they do not. */
    var readableUntil: String? = null

    /** The gradient's two ends for an author the row does not name, or null. */
    var authorColors: List<String>? = null

    /** Whether the post was written in the identified mode, under the account's public persona. */
    var identified: Boolean = false

    /** Whether the post is listed in the Everyone timeline, or only where its subjects reach. */
    var everyone: Boolean = true

    /** Live conversations rooted at this post. */
    var conversationCount: Int = 0

    /** Whether one of the conversations is the viewer's own. */
    var conversationMine: Boolean = false

    /** The conversation-local seat of the newest reply, or null. */
    var conversationLastSide: String? = null

    override var reactions: List<Reaction> = listOf()
        get() = field + listOfNotNull(
            reaction("conversation", conversationCount.takeIf { it > 0 })
        )

    private fun reaction(
        name: String,
        count: Int?,
    ): Reaction? {
        return count?.let {
            Reaction().also { reaction ->
                reaction.name = name
                reaction.count = it
            }
        }
    }

    override var webUrl: String = ""
        get() = field.ifEmpty {
            val host = service.host ?: "https://saypip.app"
            "$host/posts/${id<Any>()}".also { field = it }
        }
}
