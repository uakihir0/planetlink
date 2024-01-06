package work.socialhub.planetlink.model

/**
 * Reaction Model
 * リアクションモデル
 */
class Reaction {

    var name: String? = null
    var emoji: String? = null
    var iconUrl: String? = null
    var count: Long? = null

    /** 認証ユーザーがリアクションしたか？  */
    var reacting: Boolean = false
}
