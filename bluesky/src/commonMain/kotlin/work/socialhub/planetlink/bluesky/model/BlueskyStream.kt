package work.socialhub.planetlink.bluesky.model

import work.socialhub.kbsky.stream.entity.app.bsky.JetStreamClient
import work.socialhub.planetlink.model.Stream
import kotlin.js.JsExport

@JsExport
class BlueskyStream internal constructor(
    private val client: JetStreamClient
) : Stream {

    override suspend fun open() {
        client.open()
    }

    override fun close() {
        client.close()
    }

    override val isOpened: Boolean
        get() = client.isOpen
}
