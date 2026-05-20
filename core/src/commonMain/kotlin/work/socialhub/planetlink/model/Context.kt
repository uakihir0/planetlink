@file:Suppress("DEPRECATION")
package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * Comment Context
 */
@JsExport
class Context {

    /**
     * Get ancestor comments.
     * 特定のコメントより前のコンテキストを取得
     */
    var ancestors: List<Comment>? = null

    /**
     * Get descendant comments.
     * 特定のコメントより後のコンテキストを取得
     */
    var descendants: List<Comment>? = null

    fun sort() {
        descendants = descendants!!.sortedBy {
            it.createAt!!.toEpochMilliseconds()
        }.reversed()
        ancestors = ancestors!!.sortedBy {
            it.createAt!!.toEpochMilliseconds()
        }.reversed()
    }
}
