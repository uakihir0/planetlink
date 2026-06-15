package work.socialhub.planetlink.define

import kotlin.js.JsExport

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
