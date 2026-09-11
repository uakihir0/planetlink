package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CompletableDeferred
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

        /** An open call is installing its subscriptions. */
        const val OPENING = 1

        /** The session is installed and streaming. */
        const val OPEN = 2

        /** A close is taking the session down and publishing its teardown. */
        const val CLOSING = 3
    }

    /**
     * The stream's lifecycle state. OPENING and CLOSING are distinct from
     * OPEN and CLOSED because a concurrent call has to wait for the transition
     * that is in flight instead of mistaking the settled state for its result.
     */
    private val lifecycleState = AtomicInt(CLOSED)

    /**
     * How many [close] calls have happened. An [open] that overlapped one of
     * them gives up, even when it only waited for a teardown. The value an open
     * saw also identifies its session: a newer open bumps it, so the older one
     * can no longer mistake the newer session for itself.
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
     * Completed when the [open] that owns the OPENING state has settled. Opens
     * that arrive while it is installing wait here instead of returning before
     * the stream is ready.
     */
    private val openBarrier = AtomicReference<CompletableDeferred<Unit>?>(null)

    /**
     * The listener registration of the session that currently exists, so a
     * teardown removes exactly that one and no other.
     */
    private val activeListener = AtomicReference<((String, Boolean) -> Unit)?>(null)

    /**
     * Serializes installing subscriptions with tearing them down, so a [close]
     * that lands while [open] is still starting cannot let a subscription
     * finish installing after the teardown already ran.
     */
    private val lifecycleMutex = Mutex()

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
        val session = admitOpen() ?: return
        val closes = session.closes
        try {
            relaysConnected.store(false)
            val listener = relayStateListener(closes)
            activeListener.store(listener)
            accessor.nostr.relayPool().addRelayStateListener(listener)
            // The listener is registered before the snapshot: a transition that
            // races the two is then either reported by the listener or
            // reflected in the snapshot, never lost between them. A close() can
            // still win here, after removing a listener that was not installed
            // yet, so the stale registration is dropped again.
            if (!isSessionCurrent(closes)) {
                accessor.nostr.relayPool().removeRelayStateListener(listener)
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
                if (!isSessionCurrent(closes)) return
                timelineStream?.let { ts ->
                    val following = accessor.social.users().getFollowing(accessor.pubkey)
                    if (!isSessionCurrent(closes)) return
                    ts.start(following.data)
                    // A close() during start() wins too: it is waiting for the
                    // lock, so returning here lets its teardown stop what
                    // start() had time to install.
                    if (!isSessionCurrent(closes)) return
                }
                notificationStream?.let { ns ->
                    if (!isSessionCurrent(closes)) return
                    ns.start(accessor.pubkey)
                    if (!isSessionCurrent(closes)) return
                }
            }
            // Every subscription is installed: the session is open.
            lifecycleState.compareAndSet(OPENING, OPEN)
        } catch (e: Throwable) {
            failOpening(closes)
            throw e
        } finally {
            // Opens waiting behind this one can now decide what to do. The
            // barrier captured at admission is completed, not whatever is
            // stored now: a newer session may already have replaced it.
            session.barrier.complete(Unit)
        }
    }

    /**
     * One admitted [open] call: the [closeRequests] value it was based on and
     * the barrier concurrent opens wait on until it settles.
     */
    private class OpenSession(
        val closes: Int,
        val barrier: CompletableDeferred<Unit>,
    )

    /**
     * Wait out any transition and claim the open state.
     *
     * Returns the session the admission created, or null when another open
     * already holds the stream or a close overlapped this call; the caller then
     * leaves the stream alone.
     */
    private suspend fun admitOpen(): OpenSession? {
        while (true) {
            val closes = closeRequests.load()
            when (lifecycleState.load()) {
                // Another open() holds a ready session; this call is a no-op.
                OPEN -> return null
                // Another open() is installing; wait for it to settle, then
                // decide again (its success is a no-op, its failure is retried).
                OPENING -> openBarrier.load()?.join() ?: yield()
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
                    if (lifecycleState.compareAndSet(CLOSED, OPENING)) {
                        val session = OpenSession(closes, CompletableDeferred())
                        openBarrier.store(session.barrier)
                        return session
                    }
                    // Lost the CAS to a concurrent close or open; retry.
                }
            }
        }
    }

    /**
     * Take down the session this failed [open] call was installing. The
     * teardown is published like [close] does, so a concurrent reopen waits for
     * it instead of letting it stop the reopened streams.
     */
    private fun failOpening(closes: Int) {
        if (closes != closeRequests.load()) return
        if (!lifecycleState.compareAndSet(OPENING, CLOSING)) return
        removeActiveListener()
        stopJob.store(scope.launch {
            lifecycleMutex.withLock {
                timelineStream?.stop()
                notificationStream?.stop()
            }
        })
        lifecycleState.store(CLOSED)
    }

    /** Whether this session is still the one the stream is working on. */
    private fun isSessionCurrent(closes: Int): Boolean {
        if (closes != closeRequests.load()) return false
        val state = lifecycleState.load()
        return state == OPENING || state == OPEN
    }

    private fun removeActiveListener() {
        activeListener.load()?.let {
            accessor.nostr.relayPool().removeRelayStateListener(it)
        }
    }

    /**
     * Reports the relay pool going from "nothing reachable" to "something
     * reachable" and back.
     *
     * A subscription lives on every relay, so one relay dropping is normal and
     * says nothing about whether the stream still delivers. Only losing the last
     * one does, which is why this asks the pool instead of trusting the url it
     * was handed.
     */
    private fun relayStateListener(closes: Int): (String, Boolean) -> Unit = { _, _ ->
        // A registration can outlive its session by an instant; only the
        // session it was made for may report anything.
        if (isSessionCurrent(closes)) {
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

    override fun close() {
        closeRequests.fetchAndAdd(1)
        while (true) {
            val state = lifecycleState.load()
            if (state != OPENING && state != OPEN) return
            // Only one close may take a session down. A second closer
            // publishing a teardown would overwrite the job a reopen must wait
            // for.
            if (!lifecycleState.compareAndSet(state, CLOSING)) continue
            removeActiveListener()
            // The teardown is published before the state turns CLOSED: a reopen
            // then cannot observe the closed state without also seeing the job
            // it has to wait for, and start while this teardown is still
            // stopping the streams.
            stopJob.store(scope.launch {
                // Waiting for the lock here is what serializes this teardown
                // with a start() that is still installing.
                lifecycleMutex.withLock {
                    timelineStream?.stop()
                    notificationStream?.stop()
                }
            })
            lifecycleState.store(CLOSED)
            return
        }
    }
}
