package work.socialhub.planetlink.define

import kotlin.js.JsExport

/**
 * 対応 SNS
 * SNS List
 */
@JsExport
enum class ServiceType {
    Twitter,
    Mastodon,
    Facebook,
    Slack,
    Tumblr,
    Misskey,
    Pleroma,
    PixelFed,
    Bluesky,
    Nostr,
    Matrix,
}