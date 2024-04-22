package work.socialhub.planetlink.mastodon.define

enum class MastodonVisibility(
    val code: String
) {
    Public("public"),
    Unlisted("unlisted"),
    Private("private"),
    Direct("direct"),
    ;

    companion object {
        fun of(
            code: String
        ): MastodonVisibility {
            return entries.toTypedArray()
                .first { it.code == code }
        }
    }
}
