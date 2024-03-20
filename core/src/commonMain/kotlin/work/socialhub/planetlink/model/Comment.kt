package work.socialhub.planetlink.model

import kotlinx.datetime.Instant
import work.socialhub.planetlink.action.CommentAction
import work.socialhub.planetlink.action.CommentActionImpl
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.error.NotImplementedException
import work.socialhub.planetlink.model.request.CommentForm

/**
 * SNS コメント情報
 * SNS Comment Model
 */
open class Comment(
    service: Service
) : Identify(service) {

    var text: AttributedString? = null

    /** Date of text created  */
    var createAt: Instant? = null

    /** User who create this text  */
    var user: User? = null

    /**
     * Files which attached with this text
     * if no media with this tweet, return empty list.
     */
    var medias: List<Media> = listOf()

    /**
     * Shared text
     * (ReTweeted or Quoted text in Twitter Term)
     * (Null if text shared any text)
     */
    var sharedComment: Comment? = null

    /**
     * Is possibly sensitive?
     * NSFW in mastodon term.
     */
    var possiblySensitive: Boolean = false

    /**
     * Application which user used
     * (with application link)
     */
    var application: Application? = null

    /**
     * Is direct message comment?
     * (Comment and message are same model)
     */
    var directMessage: Boolean = false

    /**
     * Get many kind of reactions
     * (like, share, :+1:, and so on)
     */
    open val reactions: List<Reaction> = listOf()

    /**
     * Apply reaction to comment
     * (like, share, :+1:, and so on)
     */
    @Suppress("UNUSED_PARAMETER")
    fun applyReaction(reaction: Reaction) {
        throw NotImplementedException()
    }

    /**
     * Get Action
     */
    fun action(): CommentAction {
        val action = service.account.action
        return CommentActionImpl(action, this)
    }

    /**
     * Get comment should be shown
     * (Use return object to display)
     */
    open val displayComment: Comment
        get() = this

    /**
     * Get Reply Form
     * 返信用のフォームを取得
     */
    open val replyForm: CommentForm
        get() = throw NotImplementedException()


    /**
     * Get Quote Form
     * 引用RT用のフォームを取得
     */
    open val quoteForm: CommentForm
        get() = throw NotImplementedException()

    /**
     * Get Web Url
     * Web のアドレスを取得
     */
    open val webUrl: String
        get() = throw NotImplementedException()


    /**
     * Only shared content comment.
     * 共有されたコメント情報のみの場合
     */
    val isOnlyShared: Boolean
        get() = ((sharedComment != null)
                && ((text == null) || (text!!.displayText.isEmpty()))
                && (medias.isEmpty()))


    override fun equals(other: Any?): Boolean {
        return (other as? Identify?)?.let {
            isSameIdentify(it)
        } ?: false
    }
}
