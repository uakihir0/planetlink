package work.socialhub.planetlink.discord.action

import work.socialhub.kdiscord.Discord
import work.socialhub.kdiscord.DiscordFactory
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

/**
 * Discord authentication / account factory.
 *
 * kdiscord authenticates with a raw user token (self-bot style). This class
 * builds a planetlink [Account] wired to a [DiscordAction].
 */
@JsExport
class DiscordAuth(
    var apiHost: String? = null,
) : ServiceAuth<DiscordAuth.DiscordAccessor> {

    var accessToken: String? = null
    private var _accessor: DiscordAccessor? = null

    /**
     * Build an account from a user token.
     */
    fun getAccountWithToken(
        token: String
    ): Account {
        this.accessToken = token
        this._accessor = DiscordAccessor(
            discord = apiHost?.let { DiscordFactory.instance(token, it) }
                ?: DiscordFactory.instance(token),
            token = token,
        )

        return Account().also { acc ->
            acc.action = DiscordAction(acc, this)
            acc.service = Service("discord", acc).also {
                it.host = "https://discord.com/"
                it.apiHost = apiHost ?: Discord.DEFAULT_API_HOST
            }
        }
    }

    override val accessor: DiscordAccessor
        get() = checkNotNull(_accessor) {
            "Discord accessor is not initialized."
        }

    class DiscordAccessor(
        val discord: Discord,
        val token: String,
    )
}
