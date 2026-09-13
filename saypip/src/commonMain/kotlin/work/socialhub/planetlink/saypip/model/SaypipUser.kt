package work.socialhub.planetlink.saypip.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User

/**
 * Saypip user model.
 *
 * A person in Saypip has no stable public ID: [identityToken] is the viewer-scoped identity the
 * current account holds for them, valid only for this viewer and revoked with the relationship.
 * A stranger's posts carry no author at all, so a comment's `user` may be null.
 */
@JsExport
class SaypipUser(
    service: Service
) : User(service) {

    /** The viewer-scoped identity token, or empty for the authenticated account itself. */
    var identityToken: String = ""

    /** When the friendship was established, or null while it is not one. */
    var friendSince: String? = null

    /** The viewer's own picture for this person, if any. */
    var markEmoji: String? = null
    var markColor: String? = null

    /** The relationship the authenticated account holds, where the page carried one. */
    var relationship: Relationship? = null

    override var name: String = ""

    override val accountIdentify: String
        get() = identityToken

    override var webUrl: String = ""
        get() = field.ifEmpty {
            val host = service.host ?: "https://saypip.app"
            "$host/users/$identityToken".also { field = it }
        }
}
