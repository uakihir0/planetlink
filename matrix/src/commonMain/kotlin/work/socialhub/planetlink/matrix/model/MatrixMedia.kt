package work.socialhub.planetlink.matrix.model

import work.socialhub.planetlink.model.Media
import kotlin.js.JsExport

/**
 * Matrix media (an `m.room.message` attachment).
 * Matrix メディア (`m.room.message` の添付)
 *
 * The unified [sourceUrl] / [previewUrl] carry a browser-loadable HTTP URL (the
 * unauthenticated `/_matrix/media/v3` endpoint), while [sourceMxcUrl] /
 * [previewMxcUrl] keep the original `mxc://` URIs. On homeservers that disabled
 * the unauthenticated endpoints the HTTP URL 401s; a caller can then fetch the
 * bytes with the raw mxc via
 * [work.socialhub.planetlink.matrix.action.MatrixAction.resolveMedia].
 * Mirrors how [MatrixUser] keeps `avatarUrl` (raw mxc) alongside the unified
 * `iconImageUrl` (HTTP).
 */
@JsExport
class MatrixMedia : Media() {

    /** Original `mxc://` URI of the source file. */
    var sourceMxcUrl: String? = null

    /** Original `mxc://` URI of the preview image, if any. */
    var previewMxcUrl: String? = null
}
