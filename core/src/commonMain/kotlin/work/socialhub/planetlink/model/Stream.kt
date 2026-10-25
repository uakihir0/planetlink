package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * Stream
 * ストリーム操作 API
 */
@JsExport
interface Stream {
    @Throws(Throwable::class)
    suspend fun open()
    @Throws(Throwable::class)
    fun close()
    val isOpened: Boolean
}
