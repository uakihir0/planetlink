package work.socialhub.planetlink.matrix.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Space
import kotlin.js.JsExport

/**
 * Matrix Space (a room whose `m.room.create` content declares `type: "m.space"`).
 * Matrix スペース (`m.room.create` の `type` が `"m.space"` のルーム)
 *
 * Fetch a space's direct child channels by passing this instance to
 * [work.socialhub.planetlink.action.AccountAction.channels].
 */
@JsExport
class MatrixSpace(
    service: Service
) : Space(service) {

    /** The underlying Matrix room id of this space (e.g. `!abc:example.org`). */
    var roomId: String? = null

    /** Number of joined + invited members, when known. */
    var memberCount: Int? = null
}
