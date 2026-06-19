package work.socialhub.planetlink.mastodon.model

import work.socialhub.kmastodon.stream.api.EventStream
import work.socialhub.planetlink.model.Stream
import kotlin.js.JsExport

@JsExport
class MastodonStream(
    val stream: EventStream
) : Stream {

    /**
     * Set to true once the caller invokes [close], so the connection listener
     * can suppress the disconnect signal for an intentional teardown rather
     * than reporting it as a dropped connection.
     */
    var closedByCaller: Boolean = false
        private set

    override suspend fun open() {
        stream.open()
    }

    override fun close() {
        closedByCaller = true
        stream.close()
    }

    override val isOpened: Boolean
        get() = stream.isOpen()
}