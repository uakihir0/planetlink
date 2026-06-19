package work.socialhub.planetlink.bluesky.model

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import work.socialhub.kbsky.stream.entity.app.bsky.JetStreamClient
import work.socialhub.kbsky.stream.entity.callback.ClosedCallback
import work.socialhub.kbsky.stream.entity.callback.OpenedCallback
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.model.Stream
import kotlin.js.JsExport

/**
 * A Bluesky home timeline / notification stream may fan out across several
 * underlying [JetStreamClient] connections (the followed DIDs are chunked).
 * This class aggregates their lifecycle so the caller sees a single logical
 * connection: [ConnectCallback.onConnect] fires once when the first client
 * connects and [DisconnectCallback.onDisconnect] fires once when the last
 * client goes down — never per chunk, and never for a caller-initiated
 * [close].
 */
@JsExport
class BlueskyStream internal constructor(
    private val clients: List<JetStreamClient>,
    private val callback: EventCallback? = null,
) : Stream {

    private var connectEmitted = false
    private var closedByCaller = false

    init {
        clients.forEach { client ->
            client.openedCallback(object : OpenedCallback {
                override fun onOpened() {
                    if (!connectEmitted) {
                        connectEmitted = true
                        (callback as? ConnectCallback)?.onConnect()
                    }
                }
            })
            client.closedCallback(object : ClosedCallback {
                override fun onClosed() {
                    // JetStreamClient flips isOpen=false before invoking this,
                    // so none{ isOpen } means every chunk is now down.
                    if (clients.none { it.isOpen }) {
                        connectEmitted = false
                        // Suppress the disconnect signal for an intentional
                        // close() so the caller does not treat its own
                        // teardown as a dropped connection and reconnect.
                        if (!closedByCaller) {
                            (callback as? DisconnectCallback)?.onDisconnect()
                        }
                    }
                }
            })
        }
    }

    override suspend fun open() {
        coroutineScope {
            clients.forEach { client ->
                launch { client.open() }
            }
        }
    }

    override fun close() {
        closedByCaller = true
        clients.forEach { it.close() }
    }

    override val isOpened: Boolean
        get() = clients.any { it.isOpen }
}
