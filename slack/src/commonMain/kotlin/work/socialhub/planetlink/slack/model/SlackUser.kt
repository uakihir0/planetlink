package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.request.CommentForm

class SlackUser(
    service: Service
) : User(service) {

    var screenName: String? = null
    var team: SlackTeam? = null
    var isBot: Boolean = false
    var displayName: String? = null
    var email: AttributedString? = null
    var phone: AttributedString? = null
    var title: String? = null

    override var webUrl: String
        get() {
            val teamId = team?.id ?: return ""
            val userId = id?.value<String>() ?: return ""
            return "https://app.slack.com/client/$teamId/user_profile/$userId"
        }
        set(_) {}

    override val accountIdentify: String
        get() {
            val teamName = team?.name
            return if (teamName != null) "$teamName:$screenName" else screenName ?: ""
        }

    override val messageForm: CommentForm
        get() = CommentForm().also {
            it.replyId(id as? ID)
            it.isMessage(true)
        }

    override val additionalFields: MutableList<AttributedFiled>
        get() {
            val fields = mutableListOf<AttributedFiled>()
            email?.let { fields.add(AttributedFiled("Email", it)) }
            phone?.let { fields.add(AttributedFiled("Phone", it)) }
            return fields
        }
}
