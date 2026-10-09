package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.entity.UnsignedEvent
import work.socialhub.planetlink.nostr.define.NostrSigner

class NostrSignerAdapterTest {

    private val pubkey = "a".repeat(64)

    private class FakeSigner(private val pubkey: String) : NostrSigner {
        var publicKeyCalls = 0
        var signed: UnsignedEvent? = null

        override suspend fun getPublicKey(): String {
            publicKeyCalls += 1
            return pubkey
        }

        override suspend fun signEvent(event: UnsignedEvent): NostrEvent {
            signed = event
            return NostrEvent(
                id = "id-1",
                pubkey = pubkey,
                createdAt = event.createdAt,
                kind = event.kind,
                tags = event.tags,
                content = event.content,
                sig = "b".repeat(128),
            )
        }
    }

    @Test
    fun asyncSigningIsDelegated() = runTest {
        val fake = FakeSigner(pubkey)
        val adapter = NostrSignerAdapter(fake)
        val unsigned = UnsignedEvent(
            pubkey = pubkey,
            createdAt = 1_700_000_000,
            kind = 1,
            tags = listOf(listOf("t", "topic")),
            content = "hello",
        )

        val signed = adapter.signAsync(unsigned)

        assertEquals("id-1", signed.id)
        assertEquals("hello", signed.content)
        assertEquals(unsigned, fake.signed)
    }

    @Test
    fun publicKeyIsFetchedOnce() = runTest {
        val fake = FakeSigner(pubkey)
        val adapter = NostrSignerAdapter(fake)

        assertEquals(pubkey, adapter.getPublicKeyAsync())
        assertEquals(pubkey, adapter.getPublicKeyAsync())
        assertEquals(1, fake.publicKeyCalls)
    }

    @Test
    fun synchronousMembersAreUnsupported() {
        val adapter = NostrSignerAdapter(FakeSigner(pubkey))
        val unsigned = UnsignedEvent(
            pubkey = pubkey,
            createdAt = 1_700_000_000,
            kind = 1,
            tags = emptyList(),
            content = "hello",
        )

        assertFailsWith<UnsupportedOperationException> { adapter.getPublicKey() }
        assertFailsWith<UnsupportedOperationException> { adapter.sign(unsigned) }
        assertFailsWith<UnsupportedOperationException> { adapter.nip44Encrypt("x", pubkey) }
        assertFailsWith<UnsupportedOperationException> { adapter.nip04Decrypt("x", pubkey) }
    }
}
