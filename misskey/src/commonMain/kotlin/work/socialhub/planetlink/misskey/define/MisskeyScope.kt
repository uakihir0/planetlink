package work.socialhub.planetlink.misskey.define

import work.socialhub.kmisskey.entity.contant.Scope

object MisskeyScope {

    /**
     * Get all scopes.
     * 全ての権限のスコープを取得
     */
    val all: List<String>
        get() = (Scope.ALL)
            .map { it.toString() }
}