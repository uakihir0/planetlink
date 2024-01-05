package net.socialhub.planetlink.model

/**
 * Reaction Model
 * リアクションモデル
 */
class Reaction {
    //region // Getter&Setter
    var name: String? = null

    var emoji: String? = null

    var iconUrl: String? = null

    var count: Long? = null

    //endregion
    /** 認証ユーザーがリアクションしたか？  */
    var reacting: Boolean = false
}
