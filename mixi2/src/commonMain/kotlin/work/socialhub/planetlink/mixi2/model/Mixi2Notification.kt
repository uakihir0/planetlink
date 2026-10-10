package work.socialhub.planetlink.mixi2.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Service

/**
 * mixi2 notification model.
 *
 * A reaction notification carries the stamp that arrived: [reaction] is the
 * stamp id and [iconUrl] the stamp image, so a client can show which reaction
 * it was.
 */
@JsExport
class Mixi2Notification(
    service: Service
) : Notification(service) {

    /** Stamp id of the reaction that arrived, or null. */
    var reaction: String? = null

    /** Image URL of the reaction stamp, or null. */
    var iconUrl: String? = null
}
