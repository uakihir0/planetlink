package work.socialhub.planetlink.mixi2.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User

/**
 * mixi2 user (persona) model.
 *
 * mixi2 addresses people by persona id (a UUID) and by name (the `@handle`).
 * Both are carried here: [personaId] is what the API speaks, and [handle] is
 * what the web URL shows. The follow state arrives with the persona itself on
 * graph reads, and [relationship] carries it where the read answered with one.
 */
@JsExport
open class Mixi2User(
    service: Service
) : User(service) {

    /** The persona id, the identifier every mixi2 RPC speaks. */
    var personaId: String = ""

    /** The `@handle` name. */
    var handle: String = ""

    /** Whether the persona carries the verified badge. */
    var verified: Boolean = false

    /** Whether the account behind the persona is frozen. */
    var isFrozen: Boolean = false

    /** The status text shown beside the name, or null. */
    var statusText: String? = null

    /** The status icon image URL, or null. */
    var statusIconUrl: String? = null

    /** Raw following status (0 = neither, non-zero = pending or requested). */
    var followingStatus: Int = 0

    /** Number of personas this persona follows. */
    var followingCount: Long? = null

    /** Number of personas following this persona. */
    var followersCount: Long? = null

    /** The link shown on the profile, if any. */
    var link: String? = null

    /** Whether the viewer has muted this persona. */
    var isMuted: Boolean = false

    /** Whether the viewer has blocked this persona. */
    var isBlocking: Boolean = false

    /** Whether this persona has blocked the viewer. */
    var isBlocked: Boolean = false

    /** The viewer's relationship, where the read answered with a profile. */
    var relationship: Relationship? = null

    override var name: String = ""

    override val accountIdentify: String
        get() = handle.ifEmpty { personaId }

    override var webUrl: String = ""
        get() = field.ifEmpty {
            val host = service.host ?: "https://mixi.social"
            "$host/@$accountIdentify".also { field = it }
        }
}
