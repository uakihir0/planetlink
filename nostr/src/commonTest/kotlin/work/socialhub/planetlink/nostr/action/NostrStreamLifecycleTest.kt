package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import work.socialhub.knostr.NostrFactory
import work.socialhub.knostr.relay.RelayConnection
import work.socialhub.knostr.social.NostrSocialConfig
import work.socialhub.knostr.social.NostrSocialFactory
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The relays are never really opened: the tests invoke the socket listeners the
 * connection installed, which is what flips its open state.
 */
class NostrStreamLifecycleTest {

    @Test
    fun connectIsReportedOnceAndDisconnectOnlyWhenTheLastRelayDrops() = runTest {
        val accessor = accessor()
        val callback = RecordingCallback()
        val stream = NostrStream(accessor = accessor, callback = callback)
        val first = accessor.nostr.relayPool().addRelay("wss://relay-1.example")
        val second = accessor.nostr.relayPool().addRelay("wss://relay-2.example")

        stream.open()
        first.reportOpen()
        second.reportOpen()
        // A subscription lives on every relay, so the stream keeps delivering
        // while any of them is still there.
        first.reportClose()
        second.reportClose()

        assertEquals(listOf("connect", "disconnect"), callback.events)
    }

    @Test
    fun anAlreadyConnectedPoolIsReportedOnOpen() = runTest {
        val accessor = accessor()
        val callback = RecordingCallback()
        // setHomeTimeLineStream / setNotificationStream fetch the account
        // profile before open(), which usually connects a relay already.
        val relay = accessor.nostr.relayPool().addRelay("wss://relay-1.example")
        relay.reportOpen()
        val stream = NostrStream(accessor = accessor, callback = callback)

        stream.open()

        assertEquals(listOf("connect"), callback.events)
    }

    @Test
    fun aClosedStreamNoLongerReportsRelayState() = runTest {
        val accessor = accessor()
        val callback = RecordingCallback()
        val stream = NostrStream(accessor = accessor, callback = callback)
        val relay = accessor.nostr.relayPool().addRelay("wss://relay-1.example")

        stream.open()
        stream.close()
        relay.reportOpen()

        assertEquals(listOf(), callback.events)
    }

    @Test
    fun aCloseDuringTeardownCancelsThePendingOpen() = runTest {
        val accessor = accessor()
        val callback = RecordingCallback()
        val stream = NostrStream(accessor = accessor, callback = callback)
        // The teardown runs on a scheduler the test advances itself, which is
        // what holds the stream in "close started, teardown not finished".
        val teardownDispatcher = StandardTestDispatcher()
        stream.scope = CoroutineScope(SupervisorJob() + teardownDispatcher)
        val relay = accessor.nostr.relayPool().addRelay("wss://relay-1.example")

        stream.open()
        stream.close()
        val opening = launch { stream.open() }
        testScheduler.runCurrent()
        // The caller gives up while open() waits for the teardown to finish.
        stream.close()
        teardownDispatcher.scheduler.advanceUntilIdle()
        opening.join()

        assertFalse(stream.isOpened)
        // No subscription was started, so the relay state listener was never
        // installed either.
        relay.reportOpen()
        assertEquals(listOf(), callback.events)
    }

    private fun accessor(): NostrAuth.NostrAccessor {
        val nostr = NostrFactory.instance(emptyList())
        return NostrAuth.NostrAccessor(
            nostr = nostr,
            social = NostrSocialFactory.instance(nostr, NostrSocialConfig()),
            pubkey = "a".repeat(64),
        )
    }

    private fun RelayConnection.reportOpen() = client.onOpenListener(client)

    private fun RelayConnection.reportClose() = client.onCloseListener(client)

    private class RecordingCallback : ConnectCallback, DisconnectCallback {
        val events = mutableListOf<String>()

        override fun onConnect() {
            events.add("connect")
        }

        override fun onDisconnect() {
            events.add("disconnect")
        }
    }
}
