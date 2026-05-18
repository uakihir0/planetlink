package work.socialhub.planetlink.matrix.model

import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.request.CommentForm

class MatrixUser(
    service: Service
) : User(service) {

    var userId: String? = null
    var displayName: String? = null
    var avatarUrl: String? = null

    override var webUrl: String
        get() {
            val uid = userId?.removePrefix("@") ?: return ""
            return "https://matrix.to/#/$uid"
        }
        set(_) {}

    override val accountIdentify: String
        get() = userId ?: id?.value<String>() ?: ""

    override val messageForm: CommentForm
        get() = CommentForm().also {
            it.replyId(id)
            it.isMessage(true)
        }
}
