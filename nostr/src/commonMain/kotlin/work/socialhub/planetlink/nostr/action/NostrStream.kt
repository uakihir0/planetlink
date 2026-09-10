package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import work.socialhub.knostr.social.stream.NotificationStream
import work.socialhub.knostr.social.stream.TimelineStream
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.model.Stream

class NostrStream(
    private val accessor: NostrAuth.NostrAccessor,
    private val callback: EventCallback? = null,
    private var timelineStream: TimelineStream? = null,
    private var notificationStream: NotificationStream? = null,
) : Stream {

    private var _isOpened = false

    override val isOpened: Boolean
        get() = _isOpened

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stopJob: Job? = null
    private var relaysConnected = false

    /**
     * Reports the relay pool going from "nothing reachable" to "something
     * reachable" and back.
     *
     * A subscription lives on every relay, so one relay dropping is normal and
     * says nothing about whether the stream still delivers. Only losing the last
     * one does, which is why this asks the pool instead of trusting the url it
     * was handed.
     */
    private val relayStateListener: (String, Boolean) -> Unit = { _, _ ->
        val connected = accessor.nostr.relayPool().isConnected
        if (connected != relaysConnected) {
            relaysConnected = connected
            if (connected) {
                (callback as? ConnectCallback)?.onConnect()
            } else {
                (callback as? DisconnectCallback)?.onDisconnect()
            }
        }
    }

    /**
     * Open the stream.
     *
     * Opening an already-open stream does nothing. The caller of
     * `setHomeTimeLineStream` receives a stream that is already open, so a
     * caller that opens what it is given would otherwise fetch the following
     * list again, prefetch every profile again, and install a second
     * subscription whose id it never learns.
     */
    override suspend fun open() {
        if (_isOpened) return
        // A close() that is still stopping the previous subscriptions has to
        // finish first: otherwise it would tear down what is opened below.
        stopJob?.join()
        stopJob = null
        _isOpened = true
        try {
            relaysConnected = accessor.nostr.relayPool().isConnected
            accessor.nostr.relayPool().addRelayStateListener(relayStateListener)
            timelineStream?.let { ts ->
                val following = accessor.social.users().getFollowing(accessor.pubkey)
                ts.start(following.data)
            }
            notificationStream?.let { ns ->
                ns.start(accessor.pubkey)
            }
        } catch (e: Throwable) {
            _isOpened = false
            accessor.nostr.relayPool().removeRelayStateListener(relayStateListener)
            throw e
        }
    }

    override fun close() {
        if (!_isOpened) return
        _isOpened = false
        accessor.nostr.relayPool().removeRelayStateListener(relayStateListener)
        stopJob = scope.launch {
            timelineStream?.stop()
            notificationStream?.stop()
        }
    }
}
