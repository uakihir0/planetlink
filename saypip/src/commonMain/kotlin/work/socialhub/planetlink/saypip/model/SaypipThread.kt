package work.socialhub.planetlink.saypip.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread

/**
 * Saypip conversation model.
 *
 * A conversation is 1:1 and public: the viewer is one of two participants, and [users] holds
 * both seats where the viewer may be shown them.
 */
@JsExport
class SaypipThread(
    service: Service
) : Thread(service) {

    /** Whether the viewer is one of the two seats. */
    var isMine: Boolean = true

    /** Whether something was said here that the viewer has not read. */
    var unread: Boolean = false
}
