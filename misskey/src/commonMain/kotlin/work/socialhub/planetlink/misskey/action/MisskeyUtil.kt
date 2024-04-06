package work.socialhub.planetlink.misskey.action

class MisskeyUtil {

    /**
     * Default max comment length
     * (each instance changed)
     */
    var maxCommentLength = 1000f

    /**
     * {@inheritDoc}
     */
    fun commentLengthLevel(text: String): Float {
        return ((text.length.toFloat()) / maxCommentLength)
    }
}