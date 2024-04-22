package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.model.Stream
import work.socialhub.kmisskey.stream.MisskeyStream as MMisskeyStream

class MisskeyStream(
    val stream: MMisskeyStream
) : Stream {

    override suspend fun open() {
        stream.open()
    }

    override fun close() {
        stream.close()
    }

    override val isOpened: Boolean
        get() = stream.isOpen
}