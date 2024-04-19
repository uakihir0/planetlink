package work.socialhub.planetlink.mastodon.action

object MastodonUtil {

    fun commentLengthLevel(text: String): Float {
        return ((text.length.toFloat()) / 500f)
    }
}