package work.socialhub.planetlink.model

import net.socialhub.planetlink.action.AccountAction
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

    //endregion
    /**
     * Is direct message comment?
     * (Comment and message are same model)
     */
    var directMessage: Boolean = false

    val reactions: List<Reaction>
        /**
         * Get many kind of reactions
         * (like, share, :+1:, and so on)
         */
        get() = java.util.ArrayList<Reaction>()

    /**
     * Apply reaction to comment
     * (like, share, :+1:, and so on)
     */
    fun applyReaction(reaction: Reaction?) {
    }

    /**
     * Get Action
     */
    fun action(): CommentAction {
        val action: AccountAction = getService().getAccount().action()
        return CommentActionImpl(action).comment(this)
    }

    val displayComment: Comment
        /**
         * Get comment should be shown
         * (Use return object to display)
         */
        get() = this

    val replyForm: CommentForm
        /**
         * Get Reply Form
         * 返信用のフォームを取得
         */
        get() {
            throw NotImplimentedException()
        }

    val quoteForm: CommentForm
        /**
         * Get Quote Form
         * 引用RT用のフォームを取得
         */
        get() {
            throw NotImplimentedException()
        }

    val webUrl: String
        /**
         * Get Web Url
         * Web のアドレスを取得
         */
        get() {
            throw NotImplimentedException()
        }

    val isOnlyShared: Boolean
        /**
         * Only shared content comment.
         * 共有されたコメント情報のみの場合
         */
        get() = ((sharedComment != null)
                && ((text == null) || (text.getDisplayText().isEmpty()))
                && ((medias == null) || (medias!!.size == 0)))

    //region // Getter&Setter
    fun getText(): AttributedString? {
        return text
    }

    fun setText(text: AttributedString?) {
        this.text = text
    }

    fun getMedias(): List<Media>? {
        return medias
    }

    fun setMedias(medias: List<Media>?) {
        this.medias = medias
    }
}
