package work.socialhub.planetlink.mastodon.define

/**
 * Mastodon Reaction Type
 * (Action code with alias)
 */
enum class MastodonReactionType(
    vararg val codes: String
) {
    Favorite("favorite", "like"),
    Reblog("reblog", "retweet", "share"),
    Reply("reply"),
    ;
}