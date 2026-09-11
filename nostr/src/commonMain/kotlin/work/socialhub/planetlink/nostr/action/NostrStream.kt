package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /**
     * Scope the teardown of [close] runs in. It is a var so a test can hold the
     * teardown at a known point; production code never reassigns it.
     */
    internal var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stopJob: Job? = null
    private var relaysConnected = false

    /**
     * Serializes installing subscriptions with tearing them down, so a [close]
     * that lands while [open] is still starting cannot let a subscription
     * finish installing after the teardown already ran.
     */
    private val lifecycleMutex = Mutex()

    /**
     * Identifies one [open] call. Setting a stream up takes a following list and
     * a profile prefetch, and a caller that gives up on that wait closes the
     * stream while [open] is still running.
     */
    private var openGeneration = 0

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
        reportRelayState(accessor.nostr.relayPool().isConnected)
    }

    /**
     * Reports a pool state the listener or [open] observed, at most once per
     * state.
     */
    private fun reportRelayState(connected: Boolean) {
        if (connected == relaysConnected) return
        relaysConnected = connected
        if (connected) {
            (callback as? ConnectCallback)?.onConnect()
        } else {
            (callback as? DisconnectCallback)?.onDisconnect()
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
     *
     * A [close] that arrives before this returns wins: the subscriptions are
     * never started, because [close] has already run its teardown and a
     * subscription installed after it would be one nothing can stop.
     */
    override suspend fun open() {
        if (_isOpened) return
        // The generation is claimed before the wait: a close() that lands while
        // this waits for the previous teardown has to win, or this call would
        // resume and start subscriptions the caller already closed.
        val generation = ++openGeneration
        // A close() that is still stopping the previous subscriptions has to
        // finish first: otherwise it would tear down what is opened below.
        stopJob?.join()
        stopJob = null
        if (generation != openGeneration) return
        _isOpened = true
        try {
            // The listener is registered before the snapshot: a transition that
            // races the two is then either reported by the listener or
            // reflected in the snapshot, never lost between them.
            relaysConnected = false
            accessor.nostr.relayPool().addRelayStateListener(relayStateListener)
            // The pool is usually already online by the time a stream is
            // opened (a profile fetch connects it first). The listener only
            // reports the next transition, so the state it starts in has to be
            // reported here or the consumer never learns the stream connected.
            reportRelayState(accessor.nostr.relayPool().isConnected)
            lifecycleMutex.withLock {
                // A close() that arrived while this waited for the lock or for
                // the following list wins: nothing is started below.
                if (generation != openGeneration) return
                timelineStream?.let { ts ->
                    val following = accessor.social.users().getFollowing(accessor.pubkey)
                    if (generation != openGeneration) return
                    ts.start(following.data)
                    // A close() during start() wins too: it is waiting for the
                    // lock, so returning here lets its teardown stop what
                    // start() had time to install.
                    if (generation != openGeneration) return
                }
                notificationStream?.let { ns ->
                    if (generation != openGeneration) return
                    ns.start(accessor.pubkey)
                    if (generation != openGeneration) return
                }
            }
        } catch (e: Throwable) {
            if (generation == openGeneration) {
                _isOpened = false
                accessor.nostr.relayPool().removeRelayStateListener(relayStateListener)
            }
            throw e
        }
    }

    override fun close() {
        // Invalidate an open() that is waiting for the previous teardown even
        // when this looks like a no-op: otherwise it would resume and install
        // subscriptions after the caller closed the stream.
        openGeneration++
        if (!_isOpened) return
        _isOpened = false
        accessor.nostr.relayPool().removeRelayStateListener(relayStateListener)
        stopJob = scope.launch {
            // Waiting for the lock here is what serializes this teardown with
            // a start() that is still installing.
            lifecycleMutex.withLock {
                timelineStream?.stop()
                notificationStream?.stop()
            }
        }
    }
}
