package work.socialhub.planetlink.mastodon.define


/**
 * Mastodon Authorization Scopes
 * Mastodon 認証用スコープリスト
 */
object MastodonScope {
    const val READ: String = "read"
    const val WRITE: String = "write"
    const val FOLLOW: String = "follow"
    const val PUSH: String = "push"

    /** Full Access Scopes  */
    const val FULL_ACCESS: String = "$READ $WRITE $FOLLOW $PUSH"
}
