package work.socialhub.planetlink.saypip.model

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import work.socialhub.ksaypip.stream.api.RoomStream
import work.socialhub.ksaypip.stream.listener.LifeCycleListener
import work.socialhub.ksaypip.stream.listener.RoomStreamListener

class SaypipStreamTest {

    private class FakeRoomStream : RoomStream {

        var open = false

        override fun register(
            listener: RoomStreamListener,
            lifeCycle: LifeCycleListener,
        ): RoomStream {
            return this
        }

        override suspend fun open() {
            open = true
        }

        override fun close() {
            open = false
        }

        override fun isOpen(): Boolean {
            return open
        }
    }

    @Test
    fun followsTheRoom() = runTest {
        val stream = SaypipStream(FakeRoomStream())

        assertFalse(stream.isOpened)

        stream.open()
        assertTrue(stream.isOpened)

        stream.close()
        assertFalse(stream.isOpened)
    }
}
