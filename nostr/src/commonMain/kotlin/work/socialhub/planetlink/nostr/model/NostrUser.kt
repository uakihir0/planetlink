package work.socialhub.planetlink.nostr.model

import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.request.CommentForm

class NostrUser(
    service: Service
) : User(service) {

    var npub: String? = null
    var nip05: String? = null
    var lud16: String? = null
    var displayName: String? = null
    var followingCount: Int = 0
    var followersCount: Int = 0

    override var webUrl: String
        get() {
            val npubStr = npub ?: return ""
            return "https://snort.social/p/$npubStr"
        }
        set(_) {}

    override val accountIdentify: String
        get() = npub ?: id?.value<String>() ?: ""

    override val additionalFields: MutableList<AttributedFiled>
        get() {
            val fields = mutableListOf<AttributedFiled>()
            nip05?.let {
                fields.add(AttributedFiled("NIP-05", AttributedString.plain(it)))
            }
            lud16?.let {
                fields.add(AttributedFiled("LUD-16", AttributedString.plain(it)))
            }
            return fields
        }

    override val messageForm: CommentForm
        get() = CommentForm().also {
            it.replyId(id as? ID)
            it.isMessage(true)
        }
}
