package net.socialhub.planetlink.define

enum class ReactionType(vararg codes: String) {
    // List aliases of mainly actions.
    // 代表的なリアクションのエイリアス登録
    Like("like", "favorite"),
    Share("share", "retweet", "reblog"),
    Reply("reply"),
    ;

    val code: List<String> = java.util.Arrays.asList<String>(*codes)
}
