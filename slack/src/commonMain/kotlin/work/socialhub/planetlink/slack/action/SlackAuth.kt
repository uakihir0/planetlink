package work.socialhub.planetlink.slack.action

import kotlin.js.JsExport
import work.socialhub.kslack.SlackFactory
import work.socialhub.kslack.api.methods.request.oauth.OAuthV2AccessRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

@JsExport
class SlackAuth(
    var clientId: String? = null,
    var clientSecret: String? = null,
) : ServiceAuth<SlackAuth.SlackAccessor> {

    var accessToken: String? = null
    var apiHost: String? = null
    private var _accessor: SlackAccessor? = null

    companion object {
        const val AUTHORIZE_URL = "https://slack.com/oauth/v2/authorize"
    }

    /**
     * Build the OAuth v2 authorize URL.
     *
     * [scopes] are bot token scopes (`scope`) and [userScopes] are user token
     * scopes (`user_scope`). Pass [userScopes] to obtain an `xoxp-` user token,
     * which reads channels, DMs and emojis as the signed-in user.
     * Either of them may be empty, but not both.
     */
    @Throws(Throwable::class)
    fun getAuthorizationURL(
        redirectUri: String,
        scopes: String,
        state: String? = null,
        userScopes: String? = null,
    ): String {
        require(scopes.isNotEmpty() || !userScopes.isNullOrEmpty()) {
            "Either scopes or userScopes must be specified."
        }
        val params = buildString {
            append("client_id=$clientId")
            append("&redirect_uri=$redirectUri")
            if (scopes.isNotEmpty()) {
                append("&scope=$scopes")
            }
            if (!userScopes.isNullOrEmpty()) {
                append("&user_scope=$userScopes")
            }
            if (state != null) {
                append("&state=$state")
            }
        }
        return "$AUTHORIZE_URL?$params"
    }

    @Throws(Throwable::class)
    suspend fun getAccountWithCode(
        redirectUri: String,
        code: String
    ): Account {
        val slack = apiHost?.let { SlackFactory.instance(null, "$it/api/") }
            ?: SlackFactory.instance()
        val response = slack.oauth().oauthV2Access(
            OAuthV2AccessRequest(
                token = null,
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                redirectUri = redirectUri
            )
        )

        if (!response.isOk) {
            throw RuntimeException(response.error)
        }

        // Prefer the user token (requested with `user_scope`) over the bot token.
        val token = response.authedUser?.accessToken
            ?: response.accessToken
            ?: throw RuntimeException("No access token returned.")
        return getAccountWithToken(token)
    }

    @Throws(Throwable::class)
    fun getAccountWithToken(
        token: String
    ): Account {
        this.accessToken = token
        this._accessor = SlackAccessor(
            slack = apiHost?.let { SlackFactory.instance(token, "$it/api/") }
                ?: SlackFactory.instance(token),
            token = token
        )

        return Account().also { acc ->
            acc.action = SlackAction(acc, this)
            acc.service = Service("slack", acc).also {
                it.host = "https://slack.com/api/"
                it.apiHost = apiHost?.let { "$it/api/" }
            }
        }
    }

    override val accessor: SlackAccessor
        get() = checkNotNull(_accessor) {
            "Slack accessor is not initialized."
        }

    class SlackAccessor(
        val slack: work.socialhub.kslack.Slack,
        val token: String
    )
}
