package work.socialhub.planetlink.nostr.action

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

    override suspend fun open() {
        _isOpened = true
    }

    override fun close() {
        _isOpened = false
    }
}
