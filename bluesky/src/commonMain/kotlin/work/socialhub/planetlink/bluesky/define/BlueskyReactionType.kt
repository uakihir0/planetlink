package work.socialhub.planetlink.bluesky.define

/**
 * Mastodon Reaction Type
 * (Action code with alias)
 */
enum class BlueskyReactionType(
    vararg codes: String
) {
    Like("like", "favorite"),
    Repost("repost", "retweet", "share"),
    Reply("reply"),
    ;

    val codes = listOf(*codes)
}
