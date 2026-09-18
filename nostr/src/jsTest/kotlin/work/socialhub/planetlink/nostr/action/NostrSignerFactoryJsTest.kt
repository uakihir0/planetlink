package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.entity.UnsignedEvent

/**
 * The JS entry point must resolve the promises a JavaScript signer returns
 * before treating the result as a signed event.
 */
class NostrSignerFactoryJsTest {

    private val pubkey = "a".repeat(64)

    private fun unsignedEvent(): UnsignedEvent = UnsignedEvent(
        pubkey = pubkey,
        createdAt = 1_700_000_000,
        kind = 1,
        tags = listOf(listOf("t", "topic")),
        content = "hello",
    )

    @Test
    fun delayedPublicKeyPromiseIsAwaited() = runTest {
        val factory: dynamic = js(
            "(function(pubkey) { return {" +
                " getPublicKey: function() {" +
                "  return new Promise(function(resolve) { setTimeout(function() { resolve(pubkey); }, 5); });" +
                " }," +
                " signEvent: function() { return Promise.resolve(null); }" +
                "}; })"
        )
        val signer = NostrSignerFactory.fromJs(factory(pubkey))

        assertEquals(pubkey, signer.getPublicKey())
    }

    @Test
    fun delayedSigningPromiseIsAwaited() = runTest {
        val unsigned = unsignedEvent()
        val signedEvent = NostrEvent(
            id = "signed-id",
            pubkey = pubkey,
            createdAt = unsigned.createdAt,
            kind = unsigned.kind,
            tags = unsigned.tags,
            content = unsigned.content,
            sig = "c".repeat(128),
        )
        val factory: dynamic = js(
            "(function(pubkey, event) { return {" +
                " getPublicKey: function() { return Promise.resolve(pubkey); }," +
                " signEvent: function() {" +
                "  return new Promise(function(resolve) { setTimeout(function() { resolve(event); }, 5); });" +
                " }" +
                "}; })"
        )
        val signer = NostrSignerFactory.fromJs(factory(pubkey, signedEvent))

        val signed = signer.signEvent(unsigned)

        // Accessing the fields only succeeds if the promise was awaited: an
        // unresolved promise carries neither id nor content.
        assertEquals("signed-id", signed.id)
        assertEquals("hello", signed.content)
        assertEquals(unsigned.tags, signed.tags)
    }

    @Test
    fun rejectedSigningSurfacesAsFailure() = runTest {
        val factory: dynamic = js(
            "(function(pubkey) { return {" +
                " getPublicKey: function() { return Promise.resolve(pubkey); }," +
                " signEvent: function() { return Promise.reject(new Error('user rejected')); }" +
                "}; })"
        )
        val signer = NostrSignerFactory.fromJs(factory(pubkey))

        val failure = assertFailsWith<Throwable> { signer.signEvent(unsignedEvent()) }
        assertEquals("user rejected", failure.message)
    }
}
