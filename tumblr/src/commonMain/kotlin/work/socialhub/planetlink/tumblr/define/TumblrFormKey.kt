package work.socialhub.planetlink.tumblr.define

import kotlin.js.JsExport

/**
 * Keys for [work.socialhub.planetlink.model.request.CommentForm.params]
 * used by the Tumblr adapter.
 */
@JsExport
object TumblrFormKey {

    /**
     * Reblog key of the post referenced by `CommentForm.quoteId`.
     * Tumblr requires both the post id and its reblog key to reblog
     * (= quote) a post. `TumblrComment.quoteForm` fills this in.
     */
    const val REBLOG_KEY: String = "reblog_key"
}
