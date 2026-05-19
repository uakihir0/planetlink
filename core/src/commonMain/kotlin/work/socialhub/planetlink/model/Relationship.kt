package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * Relationship between accounts
 * アカウント間の関係を取得
 */
@JsExport
class Relationship {
    var followed: Boolean = false
    var following: Boolean = false
    var blocking: Boolean = false
    var muting: Boolean = false
}
