package work.socialhub.planetlink.model

/**
 * Stream
 * ストリーム操作 API
 */
interface Stream {
    fun open()
    fun close()
    val isOpened: Boolean
}
