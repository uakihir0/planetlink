package net.socialhub.planetlink.model

/**
 * Relationship between accounts
 * アカウント間の関係を取得
 */
class Relationship {
    //region // Getter&Setter
    var followed: Boolean = false

    var following: Boolean = false

    var blocking: Boolean = false

    //endregion
    var muting: Boolean = false
}
