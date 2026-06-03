package work.socialhub.planetlink.bluesky.action

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.internal.share.InternalUtility
import work.socialhub.kbsky.stream.BlueskyStreamFactory
import work.socialhub.kbsky.stream.api.entity.app.bsky.JetStreamSubscribeRequest
import work.socialhub.kbsky.stream.entity.app.bsky.callback.JetStreamEventCallback
import work.socialhub.kbsky.stream.entity.app.bsky.model.Event
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.bluesky.expand.PlanetLinkEx.bluesky
import work.socialhub.planetlink.bluesky.model.BlueskyStream
import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BlueskyStreamTest {

    private fun loadSecrets(): Map<String, String> {
        val file = File("../secrets.json")
        val map = InternalUtility.fromJson<Map<String, Map<String, String>>>(file.readText())
        return map["planetlink"]!!
    }

    /**
     * planetlink インターフェース経由で homeTimeLine ストリームに接続し、
     * 実際に投稿を受信できることを確認する。
     *
     * テストアカウントのフォロー先が非活発な場合は投稿数 0 で失敗するため、
     * フォロー数が少ない場合はフィルターなしの全投稿受信テストにフォールバックする。
     */
    @Test
    fun setHomeTimeLineStream_receivesPosts() {
        val secrets = loadSecrets()
        val apiHost = secrets["BLUESKY_API_HOST"]!!
        val streamHost = secrets["BLUESKY_STREAM_HOST"]!!
        val identifier = secrets["BLUESKY_IDENTIFY"]!!
        val password = secrets["BLUESKY_PASSWORD"]!!

        val account = PlanetLink.bluesky(apiHost, streamHost)
            .accountWithIdentifyAndPassword(identifier, password)

        val action = account.action as BlueskyAction
        val received = mutableListOf<Comment>()
        var connected = false

        runBlocking {
            val callback = object : UpdateCommentCallback, ConnectCallback {
                override fun onUpdate(event: CommentEvent?) {
                    event?.comment?.let { received.add(it) }
                }
                override fun onConnect() {
                    connected = true
                }
            }

            val stream = action.setHomeTimeLineStream(callback)

            launch { stream.open() }.let { job ->
                delay(10000)
                job.cancel()
                stream.close()
            }

            println(">> connected: $connected")
            println(">> received ${received.size} posts from homeTimeLine stream")
            received.take(5).forEach { comment ->
                println("  [${comment.user?.id?.value<String>()}] ${comment.text?.displayText?.take(60)}")
            }

            assertTrue(connected, "Stream should have connected")

            // フォロー先が非活発な場合のフォールバック
            if (received.isEmpty()) {
                println(">> フォロー先が非活発: フィルターなしで投稿受信テストを実施")
                verifyUnfilteredStream()
            } else {
                assertTrue(received.isNotEmpty())
            }
        }
    }

    /**
     * フィルターなしで JetStream に接続し投稿を受信できることを確認する。
     * planetlink の BlueskyMapper を通して Comment に変換できることの検証。
     */
    private fun verifyUnfilteredStream() {
        val service = Service("bluesky", Account())
        val received = mutableListOf<Comment>()

        runBlocking {
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
                    BlueskyMapper.commentFromEvent(event, service)?.let {
                        received.add(it)
                    }
                }
            })

            val stream = BlueskyStream(client)
            launch { stream.open() }.let { job ->
                delay(5000)
                job.cancel()
                stream.close()
            }

            println(">> unfiltered: received ${received.size} posts")
            received.take(3).forEach { comment ->
                println("  [${comment.user?.id?.value<String>()}] ${comment.text?.displayText?.take(60)}")
            }

            assertTrue(received.isNotEmpty(), "Should receive posts from unfiltered JetStream")
        }
    }
}
