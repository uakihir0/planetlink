package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.request.CommentForm
import kotlin.js.JsExport

/** Discord ユーザーモデル */
@JsExport
class DiscordUser(
    service: Service
) : User(service) {

    /** Discord username (unique-ish handle). */
    var username: String? = null

    /** Legacy discriminator ("0" if migrated). */
    var discriminator: String? = null

    /** Whether this user is a bot account. */
    var isBot: Boolean = false

    override var webUrl: String
        get() {
            val userId = id?.value<String>() ?: return ""
            return "https://discord.com/users/$userId"
        }
        set(_) {}

    override val accountIdentify: String
        get() = username ?: (id?.value<String>() ?: "")

    /** {@inheritDoc} */
    override val messageForm: CommentForm
        get() = CommentForm().also {
            it.isMessage(true)
        }
}
