package work.socialhub.planetlink.saypip.action

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.NotFoundException
import work.socialhub.planetlink.model.request.CommentForm

/**
 * What a reply does on the wire, against a local server rather than the deployment.
 *
 * Saypip has no public reply: a post may be continued only by its own author, and a reply to
 * anybody else's post is a 1:1 conversation. Which of the two a [CommentForm.replyId] names is
 * read from the post itself, so the post is what decides the write — and a conversation that
 * already exists is continued rather than refused as a second one.
 */
class SaypipReplyTest {

    private data class Recorded(
        val method: String,
        val path: String,
        val body: String,
    )

    private lateinit var server: HttpServer
    private val requests = mutableListOf<Recorded>()
    private var responder: (Recorded) -> Pair<Int, String> =
        { 404 to """{"error":{"code":"not_found"}}""" }

    private val action get() = SaypipAuth().also {
        it.host = "http://127.0.0.1:${server.address.port}"
    }.accountWithAccessToken("test-token", null).action

    @BeforeTest
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            val recorded = Recorded(
                method = exchange.requestMethod,
                path = exchange.requestURI.rawPath,
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
    fun testReplyToSomebodyElsesPostStartsAConversation() = runBlocking {
        responder = { request ->
            when (request.path) {
                "/api/posts/p_other" -> 200 to """{"id":"p_other","isMine":false}"""
                "/api/posts/p_other/conversations" -> 201 to """{"id":"cv_1"}"""
                else -> 404 to """{"error":{"code":"not_found"}}"""
            }
        }

        action.postComment(
            CommentForm().also {
                it.text = "こんにちは"
                it.replyId = ID("p_other")
            },
        )

        val write = requests.last()
        assertEquals("POST", write.method)
        assertEquals("/api/posts/p_other/conversations", write.path)
        assertEquals("""{"body":"こんにちは"}""", write.body)
        // It is not a post: nothing was created on the room's own endpoint.
        assertTrue(requests.none { it.method == "POST" && it.path == "/api/posts" })
    }

    @Test
    fun testReplyToTheReadersOwnPostStaysASelfReply() = runBlocking {
        responder = { request ->
            when (request.path) {
                "/api/posts/p_own" -> 200 to """{"id":"p_own","isMine":true}"""
                "/api/posts" -> 201 to """{"id":"p_new"}"""
                else -> 404 to """{"error":{"code":"not_found"}}"""
            }
        }

        action.postComment(
            CommentForm().also {
                it.text = "つづきです"
                it.replyId = ID("p_own")
            },
        )

        val write = requests.last()
        assertEquals("POST", write.method)
        assertEquals("/api/posts", write.path)
        assertTrue(write.body.contains("\"replyToPostId\":\"p_own\""))
        assertTrue(requests.none { it.path.endsWith("/conversations") })
    }

    @Test
    fun testALiveConversationIsContinuedRatherThanRefused() = runBlocking {
        var starts = 0
        responder = { request ->
            when (request.path) {
                "/api/posts/p_other" -> 200 to """{"isMine":false}"""
                "/api/posts/p_other/conversations" ->
                    if (request.method == "POST") {
                        starts += 1
                        409 to """{"error":{"code":"conflict","reason":"conversation_already_started"}}"""
                    } else {
                        200 to """{"items":[{"id":"cv_8","isMine":false},{"id":"cv_9","isMine":true}]}"""
                    }
                "/api/conversations/cv_9/replies" -> 201 to """{"id":"r_1"}"""
                else -> 404 to """{"error":{"code":"not_found"}}"""
            }
        }

        action.postComment(
            CommentForm().also {
                it.text = "もう一度"
                it.replyId = ID("p_other")
            },
        )

        assertEquals(1, starts)
        val write = requests.last()
        assertEquals("POST", write.method)
        assertEquals("/api/conversations/cv_9/replies", write.path)
        assertEquals("""{"body":"もう一度"}""", write.body)
    }

    @Test
    fun testAPictureOnSomebodyElsesReplyIsRefused() = runBlocking {
        responder = { request ->
            when (request.path) {
                "/api/posts/p_other" -> 200 to """{"isMine":false}"""
                else -> 404 to """{"error":{"code":"not_found"}}"""
            }
        }

        assertFailsWith<NotSupportedException> {
            action.postComment(
                CommentForm().also {
                    it.text = "しゃしん"
                    it.replyId = ID("p_other")
                    it.addImage(byteArrayOf(1, 2, 3), "a.png")
                },
            )
        }

        // Nothing beyond the post read went out: no upload, no conversation, no post.
        assertEquals(listOf("/api/posts/p_other"), requests.map { it.path })
    }

    @Test
    fun testReplyToAPostThatCannotBeReadFailsAsNotFound() = runBlocking {
        responder = { _ -> 404 to """{"error":{"code":"not_found"}}""" }

        assertFailsWith<NotFoundException> {
            action.postComment(
                CommentForm().also {
                    it.text = "こんにちは"
                    it.replyId = ID("p_gone")
                },
            )
        }

        assertEquals(listOf("/api/posts/p_gone"), requests.map { it.path })
    }
}
