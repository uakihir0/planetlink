package work.socialhub.planetlink.x.model

import kotlin.js.JsExport
import work.socialhub.planetlink.micro.MicroBlogComment
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service

@JsExport
class XComment(
    service: Service
) : MicroBlogComment(service) {

    var replyCount: Int? = null
    var bookmarkCount: Int? = null
    var quoteCount: Int? = null
    var viewCount: Long? = null
    var conversationId: String? = null
    var language: String? = null
    var article: XArticle? = null

    override var webUrl: String = ""
        get() = field.ifEmpty {
            val screenName = (user as? XUser)?.screenName.orEmpty()
            "https://x.com/$screenName/status/${id<String>()}".also { field = it }
        }

    override var reactions: List<Reaction> = listOf()
        get() = field + listOfNotNull(
            reaction("like", likeCount),
            reaction("share", shareCount),
            reaction("reply", replyCount),
            reaction("quote", quoteCount),
            reaction("bookmark", bookmarkCount),
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
}
