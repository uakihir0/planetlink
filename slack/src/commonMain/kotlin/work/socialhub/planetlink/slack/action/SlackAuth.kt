package work.socialhub.planetlink.slack.action

import work.socialhub.kslack.SlackFactory
import work.socialhub.kslack.api.methods.request.oauth.OAuthV2AccessRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

class SlackAuth(
    var clientId: String? = null,
    var clientSecret: String? = null,
) : ServiceAuth<SlackAuth.SlackAccessor> {

    var accessToken: String? = null
    private var _accessor: SlackAccessor? = null

    companion object {
        const val AUTHORIZE_URL = "https://slack.com/oauth/v2/authorize"
    }

    fun getAuthorizationURL(
        redirectUri: String,
        scopes: String
    ): String {
        val params = buildString {
            append("client_id=$clientId")
            append("&redirect_uri=$redirectUri")
            append("&scope=$scopes")
        }
        return "$AUTHORIZE_URL?$params"
    }

    suspend fun getAccountWithCode(
        redirectUri: String,
        code: String
    ): Account {
        val response = SlackFactory.instance().oauth().oauthV2Access(
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

        return getAccountWithToken(response.accessToken!!)
    }

    fun getAccountWithToken(
        token: String
    ): Account {
        this.accessToken = token
        this._accessor = SlackAccessor(
            slack = SlackFactory.instance(token),
            token = token
        )

        return Account().also { acc ->
            acc.action = SlackAction(acc, this)
            acc.service = Service("slack", acc).also {
                it.apiHost = "https://slack.com/api/"
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
