package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import work.socialhub.knostr.social.stream.NotificationStream
import work.socialhub.knostr.social.stream.TimelineStream
import work.socialhub.planetlink.model.Stream

class NostrStream(
    private var timelineStream: TimelineStream? = null,
    private var notificationStream: NotificationStream? = null,
) : Stream {

    private var _isOpened = false

    override val isOpened: Boolean
        get() = _isOpened

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun open() {
        _isOpened = true
    }

    override fun close() {
        scope.launch {
            timelineStream?.stop()
            notificationStream?.stop()
        }
        _isOpened = false
    }
}
