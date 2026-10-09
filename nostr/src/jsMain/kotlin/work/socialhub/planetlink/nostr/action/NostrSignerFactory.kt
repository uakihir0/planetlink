package work.socialhub.planetlink.nostr.action

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsExport
import kotlin.js.Promise
import kotlinx.coroutines.suspendCancellableCoroutine
import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.entity.UnsignedEvent
import work.socialhub.planetlink.nostr.define.NostrSigner

/**
 * JavaScript object shape accepted by [NostrSignerFactory.fromJs].
 *
 * A web app wraps its signer (for example `window.nostr` from a NIP-07
 * extension) in this shape, converting the Kotlin event to and from the
 * extension's own format.
 */
@JsExport
interface JsNostrSigner {
    fun getPublicKey(): Promise<String>
    fun signEvent(event: UnsignedEvent): Promise<NostrEvent>
}

/**
 * Entry point for JavaScript hosts that need to sign with a key they own.
 */
@JsExport
object NostrSignerFactory {
    fun fromJs(signer: JsNostrSigner): NostrSigner = JsNostrSignerAdapter(signer)
}

private class JsNostrSignerAdapter(
    private val signer: JsNostrSigner,
) : NostrSigner {

    override suspend fun getPublicKey(): String = signer.getPublicKey().awaitPromise()

    override suspend fun signEvent(event: UnsignedEvent): NostrEvent =
        signer.signEvent(event).awaitPromise()
}

private suspend fun <T> Promise<T>.awaitPromise(): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value -> continuation.resume(value) },
            onRejected = { error -> continuation.resumeWithException(error) },
        )
    }
