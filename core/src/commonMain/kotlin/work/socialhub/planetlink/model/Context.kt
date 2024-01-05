package net.socialhub.planetlink.model

import work.socialhub.planetlink.model.Comment

/**
 * Comment Context
 */
class Context {
    //region // Getter&Setter
    /**
     * Get ancestor comments.
     * 特定のコメントより前のコンテキストを取得
     */
    var ancestors: List<Comment>? = null

    //endregion
    /**
     * Get descendant comments.
     * 特定のコメントより後のコンテキストを取得
     */
    var descendants: List<Comment>? = null
}
