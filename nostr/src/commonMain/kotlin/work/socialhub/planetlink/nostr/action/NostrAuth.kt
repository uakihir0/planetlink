package work.socialhub.planetlink.nostr.action

import kotlin.js.JsExport
import work.socialhub.knostr.Nostr
import work.socialhub.knostr.NostrFactory
import work.socialhub.knostr.entity.Nip19Entity
import work.socialhub.knostr.social.NostrSocial
import work.socialhub.knostr.social.NostrSocialFactory
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

@JsExport
class NostrAuth(
    var relays: List<String> = listOf(),
    var nsec: String? = null,
) : ServiceAuth<NostrAuth.NostrAccessor> {

    private var _accessor: NostrAccessor? = null

    override val accessor: NostrAccessor
        get() = checkNotNull(_accessor) { "Nostr accessor is not initialized." }

    fun accountWithPrivateKey(nsec: String? = null): Account {
        val key = nsec ?: this.nsec
            ?: throw IllegalArgumentException("nsec is required for accountWithPrivateKey")

        val nostr = createNostr(relays, key)
        val social = NostrSocialFactory.instance(nostr)

        val signer = nostr.signer()
            ?: throw IllegalStateException("Signer not available without private key")
        val pubkey = signer.getPublicKey()

        this._accessor = NostrAccessor(nostr, social, pubkey)

        return Account().also { acc ->
            acc.action = NostrAction(acc, this)
            acc.service = Service("nostr", acc).also {
                it.apiHost = relays.firstOrNull() ?: "nostr"
            }
        }
    }

    /**
     * Explicitly establish relay connections.
     * Optional: NostrAction auto-connects lazily on first API call.
     * Call this if you want to pre-warm connections before making requests.
     */
    suspend fun connect() {
        val nostr = accessor.nostr
        nostr.relays().connect()
    }

    private fun createNostr(relays: List<String>, nsec: String): Nostr {
        val hexKey = decodeNsecToHex(nsec)
        return NostrFactory.instance(hexKey, relays)
    }

    private fun decodeNsecToHex(nsec: String): String {
        val temp = NostrFactory.instance(emptyList())
        val entity = temp.nip().decodeNip19(nsec)
        return when (entity) {
            is Nip19Entity.NSec -> entity.seckey
            else -> throw IllegalArgumentException("Invalid nsec format: $nsec")
        }
    }

    class NostrAccessor(
        val nostr: Nostr,
        val social: NostrSocial,
        val pubkey: String,
    )
}
