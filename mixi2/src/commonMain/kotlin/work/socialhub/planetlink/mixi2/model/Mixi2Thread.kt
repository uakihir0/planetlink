package work.socialhub.planetlink.mixi2.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread

/**
 * mixi2 chat room (thread) model.
 *
 * A room with [isGroup] false is a one-to-one conversation; its counterpart is
 * the member other than the authenticated persona.
 */
@JsExport
class Mixi2Thread(
    service: Service
) : Thread(service) {

    /** Whether the room holds a group rather than a one-to-one conversation. */
    var isGroup: Boolean = false

    /** Room title, where the room has one. */
    var title: String? = null

    /** Whether the viewer has muted the room. */
    var isMuted: Boolean = false

    /** Whether the viewer hides the room from the room list. */
    var isInvisible: Boolean = false

    /** Raw room status (accepted, requested, requesting). */
    var status: String? = null
}
