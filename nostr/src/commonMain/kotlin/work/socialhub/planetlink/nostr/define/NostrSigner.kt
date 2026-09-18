package work.socialhub.planetlink.nostr.define

import kotlin.js.JsExport
import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.entity.UnsignedEvent

/**
 * Asynchronous signing contract for a Nostr account whose key material is not
 * held by this process — a NIP-07 browser extension being the main case.
 *
 * The account never stores a private key. Every write operation awaits the
 * signer before the signed event is handed to a relay, so an implementation is
 * free to show a confirmation dialog each time it is asked to sign.
 */
@JsExport
interface NostrSigner {

    /** Hex-encoded public key (64 chars) of the signing account. */
    suspend fun getPublicKey(): String

    /** Sign an unsigned event and return the complete signed event. */
    suspend fun signEvent(event: UnsignedEvent): NostrEvent
}
