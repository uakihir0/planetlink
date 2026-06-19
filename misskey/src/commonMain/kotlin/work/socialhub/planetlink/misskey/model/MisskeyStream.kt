package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.model.Stream
import work.socialhub.kmisskey.stream.MisskeyStream as MMisskeyStream
import kotlin.js.JsExport

@JsExport
class MisskeyStream(
    val stream: MMisskeyStream
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
        get() = stream.isOpen
}
