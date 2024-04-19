package work.socialhub.planetlink.mastodon.model

import work.socialhub.planetlink.mastodon.define.MastodonVisibility
import work.socialhub.planetlink.mastodon.expand.ServiceEx.isPixelFed
import work.socialhub.planetlink.mastodon.expand.ServiceEx.isPleroma
import work.socialhub.planetlink.micro.MicroBlogComment
import work.socialhub.planetlink.model.Emoji
import work.socialhub.planetlink.model.Poll
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedString

/**
 * Mastodon Comment Model
 * Mastodon のコメント情報
 */
class MastodonComment(
    service: Service
) : MicroBlogComment(service) {

    /** Requester host  */
    var requesterHost: String? = null

    /** Warning text (Mastodon only)  */
    var spoilerText: AttributedString? = null

    /** Open range  */
    var visibility: MastodonVisibility? = null

    /** Reply count  */
    var replyCount: Int? = null

    /** emojis which contains  */
    var emojis: List<Emoji>? = null

    /** poll */
    var poll: Poll? = null

    override val webUrl: String
        get() {
            if (service.isPixelFed) {
                return ("https://"
                        + requesterHost
                        + "/p/" + user!!.accountIdentify
                        + "/" + id<String>())
            }
            if (service.isPleroma) {
                return ("https://"
                        + requesterHost
                        + "/notice/"
                        + id<String>())
            }
            return ("https://"
                    + requesterHost
                    + "/web/statuses/"
                    + id<String>())
        }


    override var reactions: List<Reaction> = listOf()
        get() = field.toMutableList()
            .also { list ->
                replyCount?.let {
                    Reaction().also {
                        it.count = replyCount
                        it.name = "reply"
                        list.add(it)
                    }
                }
            }

    override fun equals(other: Any?): Boolean {
        return if (other is MastodonComment) {
            id<String>() == other.id<String>()
        } else false
    }
}
