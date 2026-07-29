package work.socialhub.planetlink.nostr.expand

import work.socialhub.planetlink.PlanetLink
import work.socialhub.knostr.social.NostrSocialConfig
import work.socialhub.planetlink.nostr.action.NostrAuth
import kotlin.js.JsExport

@JsExport
object PlanetLinkEx {

    fun PlanetLink.Companion.nostr(
        relays: List<String>,
        nsec: String? = null,
        mediaUploadServerUrl: String = NostrSocialConfig.DEFAULT_MEDIA_UPLOAD_SERVER_URL,
    ): NostrAuth {
        return NostrAuth(
            relays = relays,
            nsec = nsec,
            nip96Server = mediaUploadServerUrl,
        )
    }
}
