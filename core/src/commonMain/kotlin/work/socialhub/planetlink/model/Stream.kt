package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * Stream
 * ストリーム操作 API
 */
@JsExport
interface Stream {
    suspend fun open()
    fun close()
    val isOpened: Boolean
}
