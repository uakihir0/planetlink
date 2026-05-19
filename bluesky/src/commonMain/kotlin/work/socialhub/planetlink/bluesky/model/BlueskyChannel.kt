package work.socialhub.planetlink.bluesky.model

import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import kotlin.js.JsExport

/**
 * Bluesky Channel Model
 * Bluesky のチャンネル情報
 */
@JsExport
class BlueskyChannel(
    service: Service
) : Channel(service) {

    var cid: String? = null
    var owner: User? = null
    var iconUrl: String? = null
    var likeCount: Int? = null
}
