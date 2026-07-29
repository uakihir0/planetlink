package work.socialhub.planetlink.nostr.action

import kotlin.test.Test
import kotlin.test.assertEquals
import work.socialhub.knostr.util.Bech32
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.nostr.expand.PlanetLinkEx.nostr

class NostrAuthTest {

    @Test
    fun factoryAcceptsMediaUploadServer() {
        val server = "https://media.example.com"

        val auth = PlanetLink.nostr(
            relays = emptyList(),
            mediaUploadServerUrl = server,
        )

        assertEquals(server, auth.mediaUploadServerUrl)
    }

    @Test
    fun mediaUploadServerIsAppliedAndUpdated() {
        val initialServer = "https://media.example.com"
        val auth = NostrAuth(
            nsec = testNsec(),
            nip96Server = initialServer,
        )

        auth.accountWithPrivateKey()

        assertEquals(initialServer, auth.mediaUploadServerUrl)
        assertEquals(initialServer, auth.accessor.social.config().mediaUploadServerUrl)

        val updatedServer = "https://uploads.example.net"
        auth.mediaUploadServerUrl = updatedServer

        assertEquals(updatedServer, auth.accessor.social.config().mediaUploadServerUrl)
        @Suppress("DEPRECATION")
        assertEquals(updatedServer, auth.nip96Server)
    }

    private fun testNsec(): String {
        val privateKey = ByteArray(32)
        privateKey[31] = 1
        return Bech32.encode("nsec", privateKey)
    }
}
