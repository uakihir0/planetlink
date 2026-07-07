package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Stream
import kotlin.js.JsExport
import work.socialhub.kdiscord.stream.DiscordStream as KDiscordStream

/**
 * planetlink [Stream] wrapper around the kdiscord Gateway client.
 */
@JsExport
class DiscordStream(
    val stream: KDiscordStream,
) : Stream {

    private var opened: Boolean = false

    override suspend fun open() {
        opened = true
        stream.start()
    }

    override fun close() {
        opened = false
        stream.stop()
    }

    override val isOpened: Boolean
        get() = stream.isConnected()
}
