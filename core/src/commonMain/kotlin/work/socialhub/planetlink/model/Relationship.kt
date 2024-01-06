package work.socialhub.planetlink.model

/**
 * Relationship between accounts
 * アカウント間の関係を取得
 */
class Relationship {
    var followed: Boolean = false
    var following: Boolean = false
    var blocking: Boolean = false
    var muting: Boolean = false
}
