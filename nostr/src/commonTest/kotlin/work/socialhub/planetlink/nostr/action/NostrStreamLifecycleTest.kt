package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.test.runTest
import work.socialhub.knostr.NostrFactory
import work.socialhub.knostr.relay.RelayConnection
import work.socialhub.knostr.social.NostrSocialConfig
import work.socialhub.knostr.social.NostrSocialFactory
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import kotlin.test.Test
import kotlin.test.assertEquals

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
