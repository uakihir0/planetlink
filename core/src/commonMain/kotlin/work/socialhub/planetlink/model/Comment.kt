package work.socialhub.planetlink.model

import net.socialhub.planetlink.action.CommentAction
import net.socialhub.planetlink.action.CommentActionImpl
import net.socialhub.planetlink.model.*
import net.socialhub.planetlink.model.common.AttributedString
import net.socialhub.planetlink.model.error.NotImplimentedException
import net.socialhub.planetlink.model.request.CommentForm

/**
 * SNS コメント情報
 * SNS Comment Model
 */
class Comment(
    service: Service
) : Identify(service) {

    var text: AttributedString? = null

    /** Date of text created  */
    var createAt: Instance? = null

    /** User who create this text  */
    var user: User? = null

    /**
     * Files which attached with this text
     * if no media with this tweet, return empty list.
     */
    var medias: List<Media>? = null

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
    val reactions: List<Reaction>? = null

    /**
     * Apply reaction to comment
     * (like, share, :+1:, and so on)
     */
    fun applyReaction(reaction: Reaction?) {
        // TODO: implement
    }

    /**
     * Get Action
     */
    fun action(): CommentAction {
        val action = service.account.action
        return CommentActionImpl(action).comment(this)
    }

    /**
     * Get comment should be shown
     * (Use return object to display)
     */
    val displayComment: Comment
        get() = this

    /**
     * Get Reply Form
     * 返信用のフォームを取得
     */
    val replyForm: CommentForm
        get() = throw NotImplimentedException()


    /**
     * Get Quote Form
     * 引用RT用のフォームを取得
     */
    val quoteForm: CommentForm
        get() = throw NotImplimentedException()

    /**
     * Get Web Url
     * Web のアドレスを取得
     */
    val webUrl: String
        get() = throw NotImplimentedException()


    /**
     * Only shared content comment.
     * 共有されたコメント情報のみの場合
     */
    val isOnlyShared: Boolean
        get() = ((sharedComment != null)
                && ((text == null) || (text!!.displayText.isEmpty()))
                && ((medias == null) || (medias!!.isEmpty())))
}
