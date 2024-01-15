package work.socialhub.planetlink.model.request

import work.socialhub.planetlink.model.ID

class CommentForm {

    /** Text  */
    var text: String? = null

    /** Warning  */
    var warning: String? = null

    /** Reply or Thread ID  */
    var replyId: ID? = null

    /** QuoteID  */
    var quoteId: ID? = null

    /** Visibility  */
    var visibility: String? = null

    /** Is Sensitive Content?  */
    var isSensitive: Boolean = false

    /** Is Message?  */
    var isMessage: Boolean = false

    /** Poll */
    var poll: PollForm? = null

    /** Images */
    var images = mutableListOf<MediaForm>()

    /** Other params */
    var params = mutableMapOf<String, Any>()

    /** Copy this object */
    fun copy(): CommentForm {
        return CommentForm().also {
            it.text(text)
            it.warning(warning)
            it.replyId(replyId)
            it.quoteId(quoteId)
            it.visibility(visibility)
            it.isSensitive(isSensitive)
            it.isMessage(isMessage)

            images.forEach { img ->
                it.addImage(img.copy())
            }
            params.forEach { (k, v) ->
                it.addParam(k, v)
            }
        }
    }

    /**
     * Set Text
     */
    fun text(text: String?): CommentForm {
        this.text = text
        return this
    }

    /**
     * Set Warning
     */
    fun warning(warning: String?): CommentForm {
        this.warning = warning
        return this
    }

    /**
     * Set Reply (Thread) ID
     */
    fun replyId(replyId: ID?): CommentForm {
        this.replyId = replyId
        return this
    }

    /**
     * Set Quote ID
     */
    fun quoteId(quoteId: ID?): CommentForm {
        this.quoteId = quoteId
        return this
    }

    /**
     * Add One File
     */
    fun addImage(data: ByteArray, name: String): CommentForm {
        return addImage(MediaForm(data, name))
    }

    /**
     * Add One Image
     */
    fun addImage(req: MediaForm): CommentForm {
        images.add(req)
        return this
    }

    /**
     * Remove One Image
     */
    fun removeImage(index: Int): CommentForm {
        images.removeAt(index)
        return this
    }

    /**
     * Set Sensitive
     */
    fun isSensitive(isSensitive: Boolean): CommentForm {
        this.isSensitive = isSensitive
        return this
    }

    /**
     * Set Message
     */
    fun isMessage(isMessage: Boolean): CommentForm {
        this.isMessage = isMessage
        return this
    }

    /**
     * Visibility
     */
    fun visibility(visibility: String?): CommentForm {
        this.visibility = visibility
        return this
    }

    /**
     * Set Poll
     */
    fun poll(poll: PollForm?): CommentForm {
        this.poll = poll
        return this
    }

    /**
     * Set addition params
     */
    fun addParam(key: String, value: Any): CommentForm {
        params[key] = value
        return this
    }
}
