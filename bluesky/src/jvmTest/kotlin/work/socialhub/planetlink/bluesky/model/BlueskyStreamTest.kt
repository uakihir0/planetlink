package work.socialhub.planetlink.bluesky.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.stream.BlueskyStreamFactory
import work.socialhub.kbsky.stream.api.entity.app.bsky.JetStreamSubscribeRequest
import work.socialhub.kbsky.stream.entity.app.bsky.callback.JetStreamEventCallback
import work.socialhub.kbsky.stream.entity.app.bsky.model.Event
import work.socialhub.planetlink.bluesky.action.BlueskyMapper
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Service
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertTrue

class BlueskyStreamTest {

    private val service = Service("bluesky", Account())

    @Test
    fun stream_receivesPosts() {
        runBlocking {
            val received = CopyOnWriteArrayList<Comment>()

            val client = BlueskyStreamFactory
                .instance()
                .jetStream()
                .subscribe(
                    JetStreamSubscribeRequest().also {
                        it.wantedCollections = listOf(BlueskyTypes.FeedPost)
                    }
                )

            client.eventCallback(object : JetStreamEventCallback {
                override fun onEvent(event: Event) {
                    val commit = event.commit ?: return
                    if (commit.operation != "create") return

                    val comment = BlueskyMapper.commentFromEvent(event, service)
                    if (comment != null) {
                        received.add(comment)
                    }
                }
            })

            val stream = BlueskyStream(listOf(client))

            launch { stream.open() }.let { job ->
                delay(5000)
                job.cancel()
                stream.close()
            }

            println(">> received ${received.size} posts")
            received.take(3).forEach { comment ->
                println("  [${comment.user?.id?.value<String>()}] ${comment.text?.displayText?.take(50)}")
            }

            assertTrue(received.isNotEmpty(), "Should receive at least one post from JetStream")
        }
    }
}
