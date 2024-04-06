package work.socialhub.planetlink.misskey.define

enum class MisskeyReactionType(
    vararg val codes: String
) {
    Favorite("favorite", "like"),
    Renote("renote", "retweet", "reblog", "share"),
    Reply("reply"),
    ;
}
