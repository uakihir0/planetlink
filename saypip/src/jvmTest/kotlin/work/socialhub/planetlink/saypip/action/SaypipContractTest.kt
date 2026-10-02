package work.socialhub.planetlink.saypip.action

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.saypip.model.SaypipComment
import work.socialhub.planetlink.saypip.model.SaypipPaging
import work.socialhub.planetlink.saypip.model.SaypipUser

/**
 * The new contract's wires, against a local server rather than the deployment: a picture on a
 * reply takes the reply's address, a post keeps the post's, the identified mode travels as an
 * optional body, and an identified persona is read through its public page.
 */
class SaypipContractTest {

    private data class Recorded(
        val method: String,
        val path: String,
        val query: String,
        val body: String,
    )

    private lateinit var server: HttpServer
    private val requests = mutableListOf<Recorded>()
    private var responder: (Recorded) -> Pair<Int, String> =
        { 404 to """{"error":{"code":"not_found"}}""" }

    private val action get() = SaypipAuth().also {
        it.host = "http://127.0.0.1:${server.address.port}"
    }.accountWithAccessToken("test-token", null).action

    private fun service(): Service {
        return Service("saypip", Account()).also {
            it.host = "https://saypip.app"
        }
    }

    private fun comment(id: String, replyId: String? = null): SaypipComment {
        return SaypipComment(service()).also {
            it.id = ID(id)
            it.replyId = replyId
            it.directMessage = replyId != null
        }
    }

    @BeforeTest
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            val recorded = Recorded(
                method = exchange.requestMethod,
                path = exchange.requestURI.rawPath,
                query = exchange.requestURI.rawQuery ?: "",
                body = exchange.requestBody.readBytes().decodeToString(),
            )
            requests += recorded
            val (status, body) = responder(recorded)
            val bytes = body.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        }
        requests.clear()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun testReactionOnAReplyTakesTheReplyAddress() = runBlocking {
        responder = { _ -> 200 to """{"reactions":[{"emoji":"🎉","count":1,"mine":true}]}""" }

        action.reactionComment(comment("r_1", replyId = "r_1"), "🎉")

        val write = requests.last()
        assertEquals("PUT", write.method)
        assertEquals("/api/replies/r_1/reactions/%F0%9F%8E%89", write.path)
        assertTrue(requests.none { it.path.startsWith("/api/posts/") })
    }

    @Test
    fun testUnreactionOnAReplyTakesTheReplyAddress() = runBlocking {
        responder = { _ -> 200 to """{"reactions":[]}""" }

        action.unreactionComment(comment("r_1", replyId = "r_1"), "🎉")

        val write = requests.last()
        assertEquals("DELETE", write.method)
        assertEquals("/api/replies/r_1/reactions/%F0%9F%8E%89", write.path)
    }

    @Test
    fun testLikeOnAReplyStillTakesTheReplyAddress() = runBlocking {
        responder = { _ -> 200 to """{"reactions":[]}""" }

        action.likeComment(comment("r_1", replyId = "r_1"))

        val write = requests.last()
        assertEquals("PUT", write.method)
        assertEquals("/api/replies/r_1/reactions/%E2%9D%A4%EF%B8%8F", write.path)
    }

    @Test
    fun testReactionOnAPostKeepsThePostAddress() = runBlocking {
        responder = { _ -> 200 to """{"reactions":[]}""" }

        action.reactionComment(comment("p_1"), "🎉")

        val write = requests.last()
        assertEquals("PUT", write.method)
        assertEquals("/api/posts/p_1/reactions/%F0%9F%8E%89", write.path)
        assertTrue(requests.none { it.path.startsWith("/api/replies/") })
    }

    @Test
    fun testPostCarriesTheEveryoneAndIdentifiedModes() = runBlocking {
        responder = { request ->
            when (request.path) {
                "/api/posts/p_own" -> 200 to """{"id":"p_own","isMine":true}"""
                else -> 201 to """{"id":"p_new"}"""
            }
        }

        action.postComment(
            CommentForm().also {
                it.text = "quiet"
                it.replyId = ID("p_own")
                it.addParam("everyone", false)
                it.addParam("identified", true)
            },
        )

        val write = requests.last()
        assertEquals("POST", write.method)
        assertEquals("/api/posts", write.path)
        assertTrue(write.body.contains("\"everyone\":false"))
        assertTrue(write.body.contains("\"identified\":true"))
    }

    @Test
    fun testConversationStartCarriesTheIdentifiedMode() = runBlocking {
        responder = { request ->
            when (request.path) {
                "/api/posts/p_other" -> 200 to """{"id":"p_other","isMine":false}"""
                else -> 201 to """{"id":"cv_1"}"""
            }
        }

        action.postComment(
            CommentForm().also {
                it.text = "hello"
                it.replyId = ID("p_other")
                it.addParam("identified", true)
            },
        )

        val write = requests.last()
        assertEquals("POST", write.method)
        assertEquals("/api/posts/p_other/conversations", write.path)
        assertEquals("""{"body":"hello","identified":true}""", write.body)
    }

    @Test
    fun testMessageCarriesTheIdentifiedMode() = runBlocking {
        responder = { _ -> 201 to """{"id":"r_1"}""" }

        action.postMessage(
            CommentForm().also {
                it.text = "hi"
                it.replyId = ID("cv_1")
                it.addParam("identified", true)
            },
        )

        val write = requests.last()
        assertEquals("POST", write.method)
        assertEquals("/api/conversations/cv_1/replies", write.path)
        assertEquals("""{"body":"hi","identified":true}""", write.body)
    }

    @Test
    fun testIdentifiedUrlReadsThePublicPage() = runBlocking {
        responder = { _ ->
            200 to """
                {
                  "handle": "foo",
                  "operator": false,
                  "profile": {"displayName": "Foo"},
                  "posts": []
                }
            """.trimIndent()
        }

        val user = action.user("https://saypip.app/identified/foo") as SaypipUser

        assertEquals("/api/identified/foo", requests.last().path)
        assertEquals("foo", user.identifiedHandle)
        assertEquals("Foo", user.name)
        assertTrue(user.verified)
    }

    @Test
    fun testIdentifiedTimelineReadsThePersonasPosts() = runBlocking {
        responder = { _ ->
            200 to """
                {
                  "handle": "foo",
                  "profile": {"displayName": "Foo"},
                  "posts": [
                    {"id": "p_1", "body": "hello", "createdAt": "2026-08-19T09:00:00.000Z", "isMine": false}
                  ],
                  "postsNextCursor": "next"
                }
            """.trimIndent()
        }

        val user = SaypipUser(service()).also {
            it.id = ID("foo")
            it.identifiedHandle = "foo"
        }
        val page = action.userCommentTimeLine(user, SaypipPaging(10))

        assertEquals("/api/identified/foo", requests.last().path)
        assertEquals(1, page.entities.size)
        assertEquals("hello", page.entities[0].text?.displayText)
        assertEquals("next", (page.paging as SaypipPaging).nextCursor)
    }

    @Test
    fun testTheIdentifiedTimelineSendsTheCursorAndLimit() = runBlocking {
        responder = { _ ->
            200 to """{"handle":"foo","profile":{},"posts":[]}"""
        }

        val user = SaypipUser(service()).also {
            it.id = ID("foo")
            it.identifiedHandle = "foo"
        }
        action.userCommentTimeLine(user, SaypipPaging(10).also { it.cursor = "c0" })

        val read = requests.last()
        assertEquals("/api/identified/foo", read.path)
        assertEquals("cursor=c0&limit=10", read.query)
    }
}
