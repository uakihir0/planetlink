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

        /** How the phase is packed into the low bits of [lifecycleState]. */
        const val PHASE_MASK = 0b11
    }

    /**
     * The stream's lifecycle, packed as `(generation shl 2) or phase`.
     *
     * The generation identifies one open call. Ending a session bumps it in
     * the same compare-and-set that changes the phase, so a transition and the
     * session it belongs to can never come apart: a stale open cannot claim a
     * new session's OPENING state and a stale teardown cannot touch the
     * session that replaced it.
     */
    private val lifecycleState = AtomicInt(CLOSED)

    /**
     * The generation and reported relay state of the current session, packed
     * as `(generation shl 1) or connected`. A listener publishes its transition
     * with a compare-and-set on this atomic, so a callback that raced a
     * teardown can neither report for a session that is gone nor mutate the
     * one that replaced it.
     */
    private val relayState = AtomicInt(0)

    /**
     * How many [close] calls have happened. An [open] reads it when it starts
     * waiting and gives up if it changed before the open was admitted, so a
     * close that overlaps the wait wins even when the lifecycle generation has
     * already moved on without it.
     */
    private val closeEpoch = AtomicInt(0)

    override val isOpened: Boolean
        get() = phaseOf(lifecycleState.load()) == OPEN

    /**
     * Scope the teardown of [close] runs in. It is a var so a test can hold the
     * teardown at a known point; production code never reassigns it.
     */
    internal var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stopJob = AtomicReference<Job?>(null)

    /**
     * The barrier of the session that most recently admitted an [open], with
     * the generation so a waiter only trusts the barrier of the OPENING
     * session it is actually waiting for.
     */
    private val openBarrier = AtomicReference<OpenBarrier?>(null)

    /**
     * The listener registration of the session that currently exists, so a
     * teardown removes exactly that one and no other.
     */
    private val activeListener = AtomicReference<ActiveListener?>(null)

    /**
     * Serializes installing subscriptions with tearing them down, so a [close]
     * that lands while [open] is still starting cannot let a subscription
     * finish installing after the teardown already ran.
     */
    private val lifecycleMutex = Mutex()

    private class ActiveListener(
        val generation: Int,
        val listener: (String, Boolean) -> Unit,
    )

    private class OpenBarrier(
        val generation: Int,
        val deferred: CompletableDeferred<Unit>,
    )

    /**
     * One admitted [open] call: its session generation and the barrier
     * concurrent opens wait on until it settles.
     */
    private class OpenSession(
        val generation: Int,
        val barrier: CompletableDeferred<Unit>,
    )

    private fun phaseOf(state: Int): Int = state and PHASE_MASK

    private fun generationOf(state: Int): Int = state ushr 2

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
        try {
            val listener = relayStateListener(session)
            publishActiveListener(session, listener)
            accessor.nostr.relayPool().addRelayStateListener(listener)
            // The listener is registered before the snapshot: a transition that
            // races the two is then either reported by the listener or
            // reflected in the snapshot, never lost between them. A close() can
            // still win here, after removing a listener that was not installed
            // yet, so the stale registration is dropped again.
            if (!isSessionCurrent(session)) {
                accessor.nostr.relayPool().removeRelayStateListener(listener)
                return
            }
            // The pool is usually already online by the time a stream is
            // opened (a profile fetch connects it first). The listener only
            // reports the next transition, so the state it starts in has to be
            // reported here or the consumer never learns the stream connected.
            reportRelayState(session, accessor.nostr.relayPool().isConnected)
            lifecycleMutex.withLock {
                // A close() that arrived while this waited for the lock or for
                // the following list wins: nothing is started below.
                if (!isSessionCurrent(session)) return
                timelineStream?.let { ts ->
                    val following = accessor.social.users().getFollowing(accessor.pubkey)
                    if (!isSessionCurrent(session)) return
                    ts.start(following.data)
                    // A close() during start() wins too: it is waiting for the
                    // lock, so returning here lets its teardown stop what
                    // start() had time to install.
                    if (!isSessionCurrent(session)) return
                }
                notificationStream?.let { ns ->
                    if (!isSessionCurrent(session)) return
                    ns.start(accessor.pubkey)
                    if (!isSessionCurrent(session)) return
                }
            }
            // Every subscription is installed. The transition is bound to this
            // session: if a close bumped the generation, the CAS fails and the
            // teardown it started takes the session down instead.
            lifecycleState.compareAndSet(
                (session.generation shl 2) or OPENING,
                (session.generation shl 2) or OPEN,
            )
        } catch (e: Throwable) {
            failOpening(session)
            throw e
        } finally {
            // Opens waiting behind this one can now decide what to do. The
            // barrier captured at admission is completed, not whatever is
            // stored now: a newer session may already have replaced it.
            session.barrier.complete(Unit)
        }
    }

    /**
     * Wait out any transition and claim the open state.
     *
     * Returns the session the admission created, or null when another open
     * already holds the stream or a close overlapped this call; the caller then
     * leaves the stream alone.
     */
    private suspend fun admitOpen(): OpenSession? {
        val epoch = closeEpoch.load()
        while (true) {
            val snapshot = lifecycleState.load()
            when (phaseOf(snapshot)) {
                // Another open() holds a ready session; this call is a no-op.
                OPEN -> return null
                // Another open() is installing; wait for the barrier of the
                // OPENING session that is currently claimed, then decide again
                // (its success is a no-op, its failure is retried).
                OPENING -> {
                    val holder = openBarrier.load()
                    if (holder != null && holder.generation == generationOf(snapshot)) {
                        holder.deferred.join()
                    } else {
                        // The barrier of this session is not published yet.
                        yield()
                    }
                    if (closeEpoch.load() != epoch) return null
                }
                // A close is publishing its teardown; wait for it and retry.
                CLOSING -> {
                    stopJob.load()?.join()
                    // A close that overlapped the wait must win over this open.
                    if (closeEpoch.load() != epoch) return null
                    yield()
                }
                else -> {
                    // The state is CLOSED, so a published teardown (written
                    // before the state) is visible here. Wait for it before
                    // claiming the session, or the teardown would stop what is
                    // opened below.
                    stopJob.load()?.join()
                    // A close that overlapped the wait must win over this open.
                    if (closeEpoch.load() != epoch) return null
                    val state = lifecycleState.load()
                    if (phaseOf(state) != CLOSED) continue
                    if (lifecycleState.compareAndSet(state, (generationOf(state) shl 2) or OPENING)) {
                        val session = OpenSession(generationOf(state), CompletableDeferred())
                        // A close can bump the epoch after the check above while
                        // the state is still CLOSED, and it then returns without
                        // tearing anything down. Recheck after claiming so that
                        // overlapping close still wins, and take the claim down
                        // here instead.
                        if (closeEpoch.load() != epoch) {
                            failOpening(session)
                            return null
                        }
                        // Publish the session-owned values with the same
                        // generation guard: a stale opener that resumes after a
                        // newer session was admitted cannot overwrite them.
                        initializeRelayState(session)
                        publishBarrier(session)
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
    private fun failOpening(session: OpenSession) {
        val opening = (session.generation shl 2) or OPENING
        val closing = ((session.generation + 1) shl 2) or CLOSING
        // Only the session that is still OPENING may be torn down here: a
        // close or a newer open has already taken over otherwise.
        if (!lifecycleState.compareAndSet(opening, closing)) return
        endSession(session.generation + 1)
    }

    /**
     * Take the session in CLOSING down: invalidate its relay state, remove its
     * listener, publish its teardown, and only then expose CLOSED.
     */
    private fun endSession(generation: Int) {
        // End the session before tearing it down: a retry then gets a new
        // generation, so a callback still in flight from the old listener
        // cannot pass the session check while the retry installs.
        relayState.store(generation shl 1)
        removeActiveListener()
        // Publish the teardown before the state turns CLOSED: a reopen then
        // cannot observe the closed state without also seeing the job it has
        // to wait for, and start while this teardown is still stopping the
        // streams.
        stopJob.store(scope.launch {
            lifecycleMutex.withLock {
                timelineStream?.stop()
                notificationStream?.stop()
            }
        })
        lifecycleState.store((generation shl 2) or CLOSED)
    }

    /**
     * Whether this session is still the one the stream is working on.
     */
    private fun isSessionCurrent(session: OpenSession): Boolean {
        val state = lifecycleState.load()
        if (generationOf(state) != session.generation) return false
        val phase = phaseOf(state)
        return phase == OPENING || phase == OPEN
    }

    /**
     * Initialize the relay state for the admitting session, unless a newer
     * session has already claimed it.
     */
    private fun initializeRelayState(session: OpenSession) {
        while (true) {
            val current = relayState.load()
            val generation = current ushr 1
            if (generation >= session.generation) return
            if (relayState.compareAndSet(current, session.generation shl 1)) return
        }
    }

    /**
     * Publish the session's barrier without letting a stale opener overwrite
     * the barrier a newer session already installed.
     */
    private fun publishBarrier(session: OpenSession) {
        while (true) {
            val current = openBarrier.load()
            if (current != null && current.generation > session.generation) return
            if (openBarrier.compareAndSet(current, OpenBarrier(session.generation, session.barrier))) return
        }
    }

    /**
     * Publish the session's listener without letting a stale open overwrite
     * the registration a newer session already installed.
     */
    private fun publishActiveListener(session: OpenSession, listener: (String, Boolean) -> Unit) {
        while (true) {
            val current = activeListener.load()
            if (current != null && current.generation > session.generation) return
            if (activeListener.compareAndSet(current, ActiveListener(session.generation, listener))) return
        }
    }

    private fun removeActiveListener() {
        activeListener.load()?.let {
            accessor.nostr.relayPool().removeRelayStateListener(it.listener)
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
    private fun relayStateListener(session: OpenSession): (String, Boolean) -> Unit = { _, _ ->
        // A registration can outlive its session by an instant. The session
        // check and the state update happen in one compare-and-set, so a
        // stale callback cannot touch the session that replaced it.
        reportRelayState(session, accessor.nostr.relayPool().isConnected)
    }

    /**
     * Reports a pool state the listener or [open] observed, at most once per
     * state.
     *
     * The session is part of the packed state: the compare-and-set only
     * succeeds while the reporting session is still the current one.
     */
    private fun reportRelayState(session: OpenSession, connected: Boolean) {
        while (true) {
            val current = relayState.load()
            if (current ushr 1 != session.generation) return
            val updated = (session.generation shl 1) or (if (connected) 1 else 0)
            if (updated == current) return
            if (relayState.compareAndSet(current, updated)) break
        }
        // The session can end between the state publish above and the callback
        // below; recheck so a callback that lost the lifecycle race is dropped.
        if (!isSessionCurrent(session)) return
        if (connected) {
            (callback as? ConnectCallback)?.onConnect()
        } else {
            (callback as? DisconnectCallback)?.onDisconnect()
        }
    }

    override fun close() {
        // Every close invalidates a pending open, even when there is no
        // session to tear down.
        closeEpoch.fetchAndAdd(1)
        while (true) {
            val state = lifecycleState.load()
            val phase = phaseOf(state)
            if (phase != OPENING && phase != OPEN) return
            val generation = generationOf(state)
            val closing = ((generation + 1) shl 2) or CLOSING
            // Only one close may take a session down, and the bump ends the
            // session generation in the same transition that starts CLOSING.
            if (!lifecycleState.compareAndSet(state, closing)) continue
            endSession(generation + 1)
            return
        }
    }
}
