package work.socialhub.planetlink.mastodon

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.mastodon.action.MastodonAction
import work.socialhub.planetlink.mastodon.action.MastodonAuth
import work.socialhub.planetlink.mastodon.model.MastodonPaging
import kotlin.test.Test
import kotlin.test.assertEquals

class MastodonListsEndpointTest {

    @Test
    fun channelsAndContainingAccountListsUseDifferentEndpoints() = runTest {
        val requestedPaths = mutableListOf<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            requestedPaths.add(path)
            val json = when (path) {
                "/api/v1/accounts/verify_credentials" ->
                    """{"id":"me","username":"test","acct":"test","url":"https://example.com/@test","display_name":"Test","note":"","avatar":"https://example.com/avatar.png","header":"https://example.com/header.png","created_at":"2024-01-01T00:00:00Z"}"""
                "/api/v1/lists" -> """[{"id":"owned","title":"Owned"}]"""
                "/api/v1/accounts/me/lists" ->
                    """[{"id":"containing","title":"Containing"}]"""

                else -> error("Unexpected request: $path")
            }
            val body = json.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            val account = MastodonAuth(
                host = "http://127.0.0.1:${server.address.port}",
                type = "MASTODON",
            ).accountWithAccessToken(
                accessToken = "access-token",
                refreshToken = null,
                expiredAt = null,
            )
            val action = account.action as MastodonAction
            val me = action.userMe()

            val owned = action.channels(me, MastodonPaging())
            val containing = action.listsContainingAccount(me)

            assertEquals("owned", owned.entities.single().id<String>())
            assertEquals("containing", containing.entities.single().id<String>())
            assertEquals(
                listOf(
                    "/api/v1/accounts/verify_credentials",
                    "/api/v1/lists",
                    "/api/v1/accounts/me/lists",
                ),
                requestedPaths,
            )
        } finally {
            server.stop(0)
        }
    }
}
