package work.socialhub.planetlink.nostr.action

import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.entity.UnsignedEvent
import work.socialhub.planetlink.nostr.define.NostrSigner

/**
 * Bridges PlanetLink's async [NostrSigner] onto knostr's signer contract.
 *
 * Only the async variants are wired: knostr signs every event through them,
 * including NIP-42 relay authentication. The synchronous members throw so a
 * caller that tries to sign off the coroutine path fails loudly instead of
 * blocking an external signer. Encryption is not part of the external contract
 * (NIP-07 extensions are not required to provide it), so those also throw.
 */
internal class NostrSignerAdapter(
    private val signer: NostrSigner,
) : work.socialhub.knostr.signing.NostrSigner {

    private var cachedPublicKey: String? = null

    override fun getPublicKey(): String = unsupported("getPublicKey")
    override fun sign(event: UnsignedEvent): NostrEvent = unsupported("sign")
    override fun computeEventId(event: UnsignedEvent): String = unsupported("computeEventId")
    override fun nip44Encrypt(plaintext: String, recipientPubkey: String): String = unsupported("nip44Encrypt")
    override fun nip44Decrypt(payload: String, senderPubkey: String): String = unsupported("nip44Decrypt")
    override fun nip04Encrypt(plaintext: String, recipientPubkey: String): String = unsupported("nip04Encrypt")
    override fun nip04Decrypt(ciphertext: String, senderPubkey: String): String = unsupported("nip04Decrypt")

    override suspend fun getPublicKeyAsync(): String {
        cachedPublicKey?.let { return it }
        return signer.getPublicKey().also { cachedPublicKey = it }
    }

    override suspend fun signAsync(event: UnsignedEvent): NostrEvent = signer.signEvent(event)

    private fun unsupported(name: String): Nothing =
        throw UnsupportedOperationException(
            "$name is not available for an external signer; use the async variant"
        )
}
