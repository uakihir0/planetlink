package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import work.socialhub.knostr.social.stream.NotificationStream
import work.socialhub.knostr.social.stream.TimelineStream
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.model.Stream
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class NostrStream(
    private val accessor: NostrAuth.NostrAccessor,
    private val callback: EventCallback? = null,
    private var timelineStream: TimelineStream? = null,
    private var notificationStream: NotificationStream? = null,
) : Stream {

    private companion object {
        /** No session is active. */
        const val CLOSED = 0

        /** A session is active. */
        const val OPEN = 1

        /** A close is taking the session down and publishing its teardown. */
        const val CLOSING = 2
    }

    /**
     * The stream's lifecycle state. A state and not a boolean, because a close
     * in progress has to keep a concurrent open from starting a session whose
     * teardown job is not published yet.
     */
    private val lifecycleState = AtomicInt(CLOSED)

    /**
     * How many [close] calls have happened. An [open] that overlapped one of
     * them gives up, even when it only waited for a teardown. Every open also
     * carries the value it saw to identify its own session: a newer open bumps
     * it, so the older one can no longer mistake the newer session for itself.
     */
    private val closeRequests = AtomicInt(0)

    override val isOpened: Boolean
        get() = lifecycleState.load() == OPEN

    /**
     * Scope the teardown of [close] runs in. It is a var so a test can hold the
     * teardown at a known point; production code never reassigns it.
     */
    internal var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stopJob = AtomicReference<Job?>(null)
    private val relaysConnected = AtomicBoolean(false)

    /**
     * Serializes installing subscriptions with tearing them down, so a [close]
     * that lands while [open] is still starting cannot let a subscription
     * finish installing after the teardown already ran.
     */
    private val lifecycleMutex = Mutex()

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
        // A registration can outlive a close by an instant; a closed stream
        // must not report anything.
        if (lifecycleState.load() == OPEN) {
            reportRelayState(accessor.nostr.relayPool().isConnected)
        }
    }

    /**
     * Reports a pool state the listener or [open] observed, at most once per
     * state.
     */
    private fun reportRelayState(connected: Boolean) {
        if (!relaysConnected.compareAndSet(!connected, connected)) return
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
        val closes = admitOpen() ?: return
        try {
            relaysConnected.store(false)
            accessor.nostr.relayPool().addRelayStateListener(relayStateListener)
            // The listener is registered before the snapshot: a transition that
            // races the two is then either reported by the listener or
            // reflected in the snapshot, never lost between them. A close() can
            // still win here, after removing a listener that was not installed
            // yet, so the stale registration is dropped again.
            if (!isCurrent(closes)) {
                accessor.nostr.relayPool().removeRelayStateListener(relayStateListener)
                return
            }
            // The pool is usually already online by the time a stream is
            // opened (a profile fetch connects it first). The listener only
            // reports the next transition, so the state it starts in has to be
            // reported here or the consumer never learns the stream connected.
            reportRelayState(accessor.nostr.relayPool().isConnected)
            lifecycleMutex.withLock {
                // A close() that arrived while this waited for the lock or for
                // the following list wins: nothing is started below.
                if (!isCurrent(closes)) return
                timelineStream?.let { ts ->
                    val following = accessor.social.users().getFollowing(accessor.pubkey)
                    if (!isCurrent(closes)) return
                    ts.start(following.data)
                    // A close() during start() wins too: it is waiting for the
                    // lock, so returning here lets its teardown stop what
                    // start() had time to install.
                    if (!isCurrent(closes)) return
                }
                notificationStream?.let { ns ->
                    if (!isCurrent(closes)) return
                    ns.start(accessor.pubkey)
                    if (!isCurrent(closes)) return
                }
            }
        } catch (e: Throwable) {
            if (closes == closeRequests.load()) {
                lifecycleState.compareAndSet(OPEN, CLOSED)
                accessor.nostr.relayPool().removeRelayStateListener(relayStateListener)
            }
            throw e
        }
    }

    /**
     * Wait out any teardown and claim the open state.
     *
     * Returns the [closeRequests] value the admission was based on, or null
     * when another open already holds the session or a close overlapped this
     * call; the caller then leaves the stream alone.
     */
    private suspend fun admitOpen(): Int? {
        while (true) {
            val closes = closeRequests.load()
            when (lifecycleState.load()) {
                // Another open() holds the session; this call is a no-op.
                OPEN -> return null
                // A close is publishing its teardown; wait for it and retry.
                CLOSING -> {
                    stopJob.load()?.join()
                    yield()
                }
                else -> {
                    // The state is CLOSED, so a published teardown (written
                    // before the state) is visible here. Wait for it before
                    // claiming the session, or the teardown would stop what is
                    // opened below.
                    stopJob.load()?.join()
                    if (closes != closeRequests.load()) return null
                    if (lifecycleState.compareAndSet(CLOSED, OPEN)) return closes
                    // Lost the CAS to a concurrent close or open; retry.
                }
            }
        }
    }

    /** Whether this open call still owns the session that admitted it. */
    private fun isCurrent(closes: Int): Boolean {
        return closes == closeRequests.load() && lifecycleState.load() == OPEN
    }

    override fun close() {
        closeRequests.fetchAndAdd(1)
        // Only one close may take a session down. A second closer publishing a
        // teardown would overwrite the job a reopen has to wait for.
        if (!lifecycleState.compareAndSet(OPEN, CLOSING)) return
        accessor.nostr.relayPool().removeRelayStateListener(relayStateListener)
        // The teardown is published before the state turns CLOSED: a reopen
        // then cannot observe the closed state without also seeing the job it
        // has to wait for, and start while this teardown is still stopping the
        // streams.
        stopJob.store(scope.launch {
            // Waiting for the lock here is what serializes this teardown with
            // a start() that is still installing.
            lifecycleMutex.withLock {
                timelineStream?.stop()
                notificationStream?.stop()
            }
        })
        lifecycleState.store(CLOSED)
    }
}
