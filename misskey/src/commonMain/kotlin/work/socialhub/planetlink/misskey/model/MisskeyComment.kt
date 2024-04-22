package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.micro.MicroBlogComment
import work.socialhub.planetlink.misskey.define.MisskeyVisibility
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Poll
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.request.CommentForm

/**
 * Misskey Comment Model
 * Misskey のコメントモデル
 */
class MisskeyComment(
    service: Service
) : MicroBlogComment(service) {

    /** Requester host  */
    var requesterHost: String? = null

    /** ID for Paging  */
    var pagingId: String? = null

    /** Warning text */
    var spoilerText: AttributedString? = null

    /** Visibility  */
    var visibility: MisskeyVisibility? = null

    /** User replied this comment  */
    var replyCount: Int? = null

    /** Poll */
    var poll: Poll? = null

    /** Reactions  */
    override var reactions: List<Reaction> = listOf()
        get() {
            return field.toMutableList()
                .also { list ->
                    Reaction().also {
                        it.count = replyCount
                        it.name = "reply"
                        list.add(it)
                    }
                    Reaction().also {
                        it.reacting = shared
                        it.count = shareCount
                        it.name = "share"
                        list.add(it)
                    }
                }
        }

    override val webUrl: String
        get() = ("https://"
                + requesterHost
                + "/notes/"
                + id<String>().toString())


    override val displayComment: Comment
        get() {
            // Misskey の場合コメントが必ず入るので空文字か確認
            return sharedComment?.let {
                val text = (text?.displayText ?: "")
                if (text.isEmpty()) it else this
            } ?: this
        }

    override val quoteForm: CommentForm
        get() {
            val form = CommentForm()
            form.isMessage(false)
            form.quoteId(id)
            return form
        }

    val idForPaging: String
        get() = pagingId ?: id<String>()

    override fun equals(other: Any?): Boolean {
        return if (other is MisskeyComment) {
            return id<String>() == other.id<String>()
        } else false
    }
}