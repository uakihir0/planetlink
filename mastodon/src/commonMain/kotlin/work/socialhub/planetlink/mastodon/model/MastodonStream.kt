package work.socialhub.planetlink.mastodon.model

import work.socialhub.kmastodon.stream.api.EventStream
import work.socialhub.planetlink.model.Stream

class MastodonStream(
    val stream: EventStream
) : Stream {

    override suspend fun open() {
        stream.open()
    }

    override fun close() {
        stream.close()
    }

    override val isOpened: Boolean
        get() = stream.isOpen()
}