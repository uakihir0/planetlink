package net.socialhub.planetlink.model.request

class CommentForm {
    // ============================================================== //
    // Fields
    // ============================================================== //
    // ============================================================== //
    // Getters
    // ============================================================== //
    //region // Getter&Setter
    /** Text  */
    var text: String? = null
        private set

    /** Warning  */
    var warning: String? = null
        private set

    /** Reply or Thread ID  */
    var replyId: Any? = null
        private set

    /** QuoteID  */
    var quoteId: Any? = null
        private set

    /** Images  */
    private var images: MutableList<MediaForm>? = null

    /** Is Sensitive Content?  */
    var isSensitive: Boolean = false
        private set

    /** Is Message?  */
    var isMessage: Boolean = false
        private set

    /** Visibility  */
    var visibility: String? = null
        private set

    /** Poll  */
    private var poll: PollForm? = null

    /** Other params  */
    private var params: MutableMap<String, Any?>? = null

    /** Copy this object  */
    fun copy(): CommentForm {
        val form = CommentForm()
        form.text(text)
        form.warning(warning)
        form.replyId(replyId)
        form.quoteId(quoteId)
        form.sensitive(isSensitive)
        form.message(isMessage)
        form.visibility(visibility)

        if (images != null) {
            for (image in images) {
                form.addImage(image.copy())
            }
        }
        if (params != null) {
            for (key in params!!.keys) {
                form.param(key, params!![key])
            }
        }
        return form
    }

    // ============================================================== //
    // Functions
    // ============================================================== //
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
    fun replyId(replyId: Any?): CommentForm {
        this.replyId = replyId
        return this
    }

    /**
     * Set Quote ID
     */
    fun quoteId(quoteId: Any?): CommentForm {
        this.quoteId = quoteId
        return this
    }

    /**
     * Add One Image
     */
    fun addImage(image: ByteArray?, name: String?): CommentForm {
        val req: MediaForm = MediaForm()
        req.setData(image)
        req.setName(name)
        return addImage(req)
    }

    /**
     * Add One Image
     */
    fun addImage(req: MediaForm): CommentForm {
        if (this.images == null) {
            this.images = java.util.ArrayList<MediaForm>()
        }

        images!!.add(req)
        return this
    }

    /**
     * s
     * Remove One Image
     */
    fun removeImage(index: Int): CommentForm {
        images!!.removeAt(index)
        return this
    }

    /**
     * Set Sensitive
     */
    fun sensitive(isSensitive: Boolean): CommentForm {
        this.isSensitive = isSensitive
        return this
    }

    /**
     * Set Message
     */
    fun message(isMessage: Boolean): CommentForm {
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
    fun param(key: String, value: Any?): CommentForm {
        if (this.params == null) {
            this.params = java.util.HashMap<String, Any>()
        }
        params!![key] = value
        return this
    }

    fun getImages(): List<MediaForm>? {
        return images
    }

    fun getPoll(): PollForm? {
        return poll
    }

    fun getParams(): Map<String, Any?> {
        if (params == null) {
            return emptyMap<String, Any>()
        }
        return params
    } //endregion
}
