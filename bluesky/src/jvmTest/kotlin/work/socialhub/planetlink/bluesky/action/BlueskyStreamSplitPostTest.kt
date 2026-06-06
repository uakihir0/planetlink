package work.socialhub.planetlink.bluesky.action

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.bluesky.expand.PlanetLinkEx.bluesky
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.model.Comment
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertTrue

class BlueskyStreamSplitPostTest {

    @Test
    fun testSplitConnectionReceivesOwnPost() {
        val account = PlanetLink.bluesky("https://bsky.social", null)
            .accountWithIdentifyAndPassword("uakihir0.com", "infh-3zpn-t3zt-eqjn")

        val action = account.action as BlueskyAction
        val received = CopyOnWriteArrayList<Comment>()
        val connectCount = AtomicInteger(0)
        val allConnected = AtomicBoolean(false)

        runBlocking {
            val callback = object : UpdateCommentCallback, ConnectCallback {
                override fun onUpdate(event: CommentEvent?) {
                    event?.comment?.let { received.add(it) }
                }
                override fun onConnect() {
                    val count = connectCount.incrementAndGet()
                    println(">> onConnect ($count)")
                    if (count >= 11) {
                        allConnected.set(true)
                        println(">> all connections established")
                    }
                }
            }

            val stream = action.setHomeTimeLineStream(callback)

            val job = launch { stream.open() }

            // 全接続が確立するまで待つ
            var waited = 0
            while (!allConnected.get() && waited < 40000) {
                delay(500)
                waited += 500
            }
            println(">> connections: ${connectCount.get()}, waited ${waited}ms")

            // 投稿する
            val testText = "[planetlink-test] split stream verify ${System.currentTimeMillis()}"
            val form = CommentForm().also { it.text(testText) }
            action.postComment(form)
            println(">> posted: $testText")

            // 受信待ち
            delay(10000)

            job.cancel()
            stream.close()

            println(">> received ${received.size} posts")
            received.forEach { c ->
                println("  [${c.user?.id?.value<String>()}] ${c.text?.displayText?.take(80)}")
            }

            val myPost = received.find { it.text?.displayText?.contains("split stream verify") == true }
            assertTrue(myPost != null,
                "Should receive own post via split stream (received ${received.size} total)")

            // テスト投稿削除
            if (myPost != null) {
                try {
                    action.deleteComment(myPost)
                    println(">> test post deleted")
                } catch (e: Exception) {
                    println(">> failed to delete: ${e.message}")
                }
            }
        }
    }
}
