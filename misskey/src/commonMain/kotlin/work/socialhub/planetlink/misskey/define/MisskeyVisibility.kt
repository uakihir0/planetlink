package work.socialhub.planetlink.misskey.define

enum class MisskeyVisibility(
    val code: String
) {
    Public("public"),
    Home("home"),
    Followers("followers"),
    Specified("specified"),
    Message("message"),
    ;

    companion object {
        fun of(code: String): MisskeyVisibility {
            return entries.toTypedArray()
                .first { it.code == code }
        }
    }
}