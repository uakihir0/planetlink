package work.socialhub.planetlink.tumblr.define

/**
 * Tumblr Reaction Type
 * (Action code with alias)
 */
enum class TumblrReactionType(
    vararg val codes: String
) {
    Like("like", "favorite"),
    Reblog("reblog", "retweet", "share"),
    Reply("reply"),
    ;
}