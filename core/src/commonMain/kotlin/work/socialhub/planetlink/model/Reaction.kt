package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * Reaction Model
 * リアクションモデル
 */
@JsExport
class Reaction {

    var name: String? = null
    var emoji: String? = null
    var iconUrl: String? = null
    var count: Int? = null

    /** 認証ユーザーがリアクションしたか？  */
    var reacting: Boolean = false
}
