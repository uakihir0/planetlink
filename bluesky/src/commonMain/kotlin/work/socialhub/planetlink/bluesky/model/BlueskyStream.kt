package work.socialhub.planetlink.bluesky.model

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import work.socialhub.kbsky.stream.entity.app.bsky.JetStreamClient
import work.socialhub.planetlink.model.Stream
import kotlin.js.JsExport

@JsExport
class BlueskyStream internal constructor(
    private val clients: List<JetStreamClient>
) : Stream {

    constructor(client: JetStreamClient) : this(listOf(client))

    override suspend fun open() {
        coroutineScope {
            clients.forEach { client ->
                launch { client.open() }
            }
        }
    }

    override fun close() {
        clients.forEach { it.close() }
    }

    override val isOpened: Boolean
        get() = clients.any { it.isOpen }
}
