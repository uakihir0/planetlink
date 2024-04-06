package work.socialhub.planetlink.bluesky.model

import work.socialhub.kbsky.model.atproto.repo.RepoStrongRef
import work.socialhub.kbsky.util.ATUriParser
import work.socialhub.planetlink.micro.MicroBlogComment
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm

/**
 * Bluesky Comment Model
 * Bluesky のコメント情報
 */
class BlueskyComment(
    service: Service
) : MicroBlogComment(service) {

    var cid: String? = null

    /** Is Simple Object  */
    var simple: Boolean? = null

    /** Reply count  */
    var replyCount: Int = 0

    var likeRecordUri: String? = null

    var repostRecordUri: String? = null

    var replyRootTo: Identify? = null

    override val webUrl: String
        get() {
            val user = user as BlueskyUser
            val rkey = ATUriParser.getRKey(id!!.value())
            return "https://bsky.app/profile/${user.screenName}/post/${rkey}"
        }

    override var reactions: List<Reaction> = listOf()
        get() {
            return super.reactions.toMutableList()
                .also { list ->
                    Reaction().also {
                        it.count = replyCount
                        it.name = "reply"
                        list.add(it)
                    }
                }
        }

    override val quoteForm
        get() = CommentForm()
            .quoteId(id)
            .isMessage(false)

    /**
     * Bluesky では handle をリプライの本文に含める必要はないため上書きする
     * In Bluesky, there is no need to include the handle in the reply body.
     */
    override val replyForm
        get() = CommentForm()
            .text("")
            .replyId(id)
            .isMessage(false)


    fun ref(): RepoStrongRef {
        return RepoStrongRef(
            uri = id!!.value(),
            cid = cid!!,
        )
    }
}
