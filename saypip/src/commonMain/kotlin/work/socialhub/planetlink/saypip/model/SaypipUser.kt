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
 *
 * An identified persona is the one exception: it is the same for every reader, so it arrives with
 * no [identityToken] and carries [identifiedHandle] instead. The two are never present together.
 */
@JsExport
open class SaypipUser(
    service: Service
) : User(service) {

    /** The viewer-scoped identity token, or empty for the authenticated account itself. */
    var identityToken: String = ""

    /** The public handle of an identified persona, or null on every viewer-scoped person. */
    var identifiedHandle: String? = null

    /** Whether the identified persona carries the verified badge. */
    var verified: Boolean = false

    /** Whether the account behind the persona is one of the deployment's operators. */
    var operator: Boolean = false

    /** When the friendship was established, or null while it is not one. */
    var friendSince: String? = null

    /** Whether the viewer keeps this person's writing in the connections timeline. */
    var watching: Boolean = false

    /** The viewer's own memo about this person, where the page carried one. */
    var note: String? = null

    /** The viewer's own picture for this person, if any. */
    var markEmoji: String? = null

    /** The gradient's two ends, top left then bottom right, or null. */
    var markColors: List<String>? = null

    /** The relationship the authenticated account holds, where the page carried one. */
    var relationship: Relationship? = null

    override var name: String = ""

    override val accountIdentify: String
        get() = identityToken

    override var webUrl: String = ""
        get() = field.ifEmpty {
            val host = service.host ?: "https://saypip.app"
            val path = identifiedHandle
                ?.let { "identified/$it" }
                ?: "users/$identityToken"
            "$host/$path".also { field = it }
        }
}
