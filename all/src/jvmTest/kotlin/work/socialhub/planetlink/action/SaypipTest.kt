package work.socialhub.planetlink.action

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import net.socialhub.planetlink.model.event.CommentEvent
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.saypip.expand.PlanetLinkEx.saypip
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.error.SocialHubException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Integration tests for the Saypip adapter. They need a `secrets.json` whose `planetlink` block
 * carries `SAYPIP_HOST` and `SAYPIP_ACCESS_TOKEN`; nothing here reaches the deployment without
 * one.
 */
class SaypipTest {

    @Nested
    inner class Me : AbstractTest() {
        @Test
        fun testSaypip() = runTest {
            dump(saypip().action.userMe())
        }
    }

    @Nested
    inner class HomeTimeLine : AbstractTest() {
        @Test
        fun testSaypip() = runTest {
            val page = saypip().action.homeTimeLine(Paging(10))
            println("Home time line count: ${page.entities.size}")
            println("Has past page: ${page.paging?.isHasPast}")
        }
    }

    @Nested
    inner class Notifications : AbstractTest() {
        @Test
        fun testSaypip() = runTest {
            val page = saypip().action.notification(Paging(10))
            println("Notification count: ${page.entities.size}")
            page.entities.forEach { println(it.type) }
        }
    }

    /**
     * A refusal that cannot be refreshed is still the adapter's own exception, not a raw client
     * one: the refresh is classified like the request that led to it.
     */
    @Nested
    inner class RefreshFailure : AbstractTest() {
        @Test
        fun testSaypip(): Unit = runBlocking {
            val c = checkNotNull(config)
            if (c["SAYPIP_HOST"]?.isEmpty() != false && c["SAYPIP_SERVER"]?.isEmpty() != false) {
                return@runBlocking
            }

            val account = PlanetLink.saypip(
                checkNotNull(c["SAYPIP_HOST"] ?: c["SAYPIP_SERVER"]),
            )
                .setConsumerInfo(c["SAYPIP_CLIENT_ID"] ?: "", c["SAYPIP_CLIENT_SECRET"])
                .accountWithAccessToken("an-expired-token", "a-revoked-refresh-token")

            val exception = assertFailsWith<SocialHubException> {
                account.action.userMe()
            }
            println("REFRESH-FAIL service=${exception.serviceType} type=${exception.error}")
            assertEquals(ServiceType.Saypip, exception.serviceType)
        }
    }

    /**
     * The room, through the adapter: a post is written while the socket is open, the frame is
     * read back as a whole comment, and the post is taken down again.
     */
    @Nested
    inner class Stream : AbstractTest() {
        @Test
        fun testSaypip(): Unit = runBlocking {
            val account = saypip()
            val action = account.action
            val body = "planetlink saypip stream test ${System.currentTimeMillis()}"

            val connected = CompletableDeferred<Unit>()
            val received = CompletableDeferred<Comment>()

            val stream = action.setHomeTimeLineStream(
                object :
                    UpdateCommentCallback,
                    ConnectCallback {
                    override fun onUpdate(event: CommentEvent?) {
                        val comment = event?.comment ?: return
                        if (comment.text?.displayText == body) {
                            received.complete(comment)
                        }
                    }

                    override fun onConnect() {
                        connected.complete(Unit)
                    }
                },
            )

            val opening = launch { stream.open() }
            try {
                withTimeout(15_000) { connected.await() }
                println("STREAM connected")

                action.postComment(CommentForm().also { it.text = body })

                val comment = withTimeout(20_000) { received.await() }
                assertNotNull(comment.id)
                println("STREAM comment=${comment.id<String>()} author=${comment.user?.name}")

                // Clean up: the post is taken down, and it uses the id the frame gave us.
                action.deleteComment(
                    Identify(account.service, ID(comment.id<String>())),
                )
                println("STREAM cleaned up")
            } finally {
                stream.close()
                opening.cancel()
            }
        }
    }
}
