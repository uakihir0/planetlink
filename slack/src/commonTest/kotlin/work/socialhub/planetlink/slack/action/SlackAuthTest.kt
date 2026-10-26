package work.socialhub.planetlink.slack.action

import kotlin.test.Test
import kotlin.test.assertEquals

class SlackAuthTest {

    @Test
    fun buildsBotScopeUrlAsBefore() {
        val url = SlackAuth(clientId = "id").getAuthorizationURL(
            redirectUri = "http://localhost:8080/",
            scopes = "channels:read",
            state = "s",
        )
        assertEquals(
            "https://slack.com/oauth/v2/authorize?client_id=id&redirect_uri=http://localhost:8080/&scope=channels:read&state=s",
            url,
        )
    }

    @Test
    fun buildsUserScopeUrl() {
        val url = SlackAuth(clientId = "id").getAuthorizationURL(
            redirectUri = "http://localhost:8080/",
            scopes = "",
            state = "s",
            userScopes = "channels:read,im:read",
        )
        assertEquals(
            "https://slack.com/oauth/v2/authorize?client_id=id&redirect_uri=http://localhost:8080/&user_scope=channels:read,im:read&state=s",
            url,
        )
    }
}
