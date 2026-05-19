package work.socialhub.planetlink.action.group

import work.socialhub.planetlink.model.group.CommentGroup
import kotlin.js.JsExport

@JsExport
interface CommentsRequestGroupAction {

    /**
     * Get Comments.
     * default count is 200.
     */
    suspend fun comments(count: Int = 200): CommentGroup
}
