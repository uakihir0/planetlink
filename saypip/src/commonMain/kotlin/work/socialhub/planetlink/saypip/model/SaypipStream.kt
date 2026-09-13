package work.socialhub.planetlink.saypip.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.js.JsExport
import work.socialhub.ksaypip.stream.api.RoomStream
import work.socialhub.planetlink.model.Stream

/**
 * Saypip's Global Room, as a PlanetLink stream.
 *
 * A frame is a notification and not a post, so the adapter reads the post back through the API
 * before handing it to a listener; that read and the callback run on [scope], which [close]
 * cancels along with the socket.
 */
@JsExport
class SaypipStream(
    val stream: RoomStream,
) : Stream {

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun open() {
        stream.open()
    }

    override fun close() {
        stream.close()
        scope.cancel()
    }

    override val isOpened: Boolean
        get() = stream.isOpen()
}
