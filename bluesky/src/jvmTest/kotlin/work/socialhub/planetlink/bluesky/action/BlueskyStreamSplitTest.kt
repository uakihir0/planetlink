package work.socialhub.planetlink.bluesky.action

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.bluesky.expand.PlanetLinkEx.bluesky
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.model.Comment
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertTrue

class BlueskyStreamSplitTest {

    @Test
    fun testSplitConnectionWithManyFollows() {
        val account = PlanetLink.bluesky("https://bsky.social", null)
            .accountWithIdentifyAndPassword("uakihir0.com", "infh-3zpn-t3zt-eqjn")

        val action = account.action as BlueskyAction
        val received = CopyOnWriteArrayList<Comment>()
        val connectCount = AtomicInteger(0)
        val connected = AtomicBoolean(false)

        runBlocking {
            val callback = object : UpdateCommentCallback, ConnectCallback {
                override fun onUpdate(event: CommentEvent?) {
                    event?.comment?.let { received.add(it) }
                }
                override fun onConnect() {
                    val count = connectCount.incrementAndGet()
                    connected.set(true)
                    println(">> onConnect called ($count times)")
                }
            }

            val stream = action.setHomeTimeLineStream(callback)

            launch { stream.open() }.let { job ->
                delay(15000)
                job.cancel()
                stream.close()
            }

            println(">> connected: ${connected.get()}")
            println(">> onConnect count: ${connectCount.get()} (= number of split connections)")
            println(">> received ${received.size} posts")
            received.take(5).forEach { comment ->
                println("  [${comment.user?.id?.value<String>()}] ${comment.text?.displayText?.take(60)}")
            }

            assertTrue(connected.get(), "Stream should have connected")
            assertTrue(connectCount.get() > 1, "Should have multiple connections (got ${connectCount.get()})")
            println(">> VERIFIED: split connections working (${connectCount.get()} connections)")
        }
    }
}
