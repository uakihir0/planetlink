package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import work.socialhub.knostr.EventKind
import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.entity.UnsignedEvent
import work.socialhub.knostr.util.Bech32
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.nostr.define.NostrSigner
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

    @Test
    fun accountWithSignerUsesExternalSigner() = runTest {
        val pubkey = "a".repeat(64)
        val fake = object : NostrSigner {
            var publicKeyCalls = 0

            override suspend fun getPublicKey(): String {
                publicKeyCalls += 1
                return pubkey
            }

            override suspend fun signEvent(event: UnsignedEvent): NostrEvent {
                return NostrEvent(
                    id = "signed-id",
                    pubkey = pubkey,
                    createdAt = event.createdAt,
                    kind = event.kind,
                    tags = event.tags,
                    content = event.content,
                    sig = "b".repeat(128),
                )
            }
        }

        val auth = NostrAuth(relays = listOf("wss://relay.example.com"))
        auth.accountWithSigner(fake)

        assertEquals(pubkey, auth.accessor.pubkey)
        assertEquals(1, fake.publicKeyCalls)
        assertTrue(auth.accessor.nostr.config().autoAuth)

        val signer = auth.accessor.nostr.signer()!!
        val unsigned = UnsignedEvent(
            pubkey = pubkey,
            createdAt = 1_700_000_000,
            kind = EventKind.TEXT_NOTE,
            content = "hello",
        )
        assertEquals("signed-id", signer.signAsync(unsigned).id)
        assertFailsWith<UnsupportedOperationException> { signer.sign(unsigned) }
    }

    private fun testNsec(): String {
        val privateKey = ByteArray(32)
        privateKey[31] = 1
        return Bech32.encode("nsec", privateKey)
    }
}
