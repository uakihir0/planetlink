package work.socialhub.planetlink.bluesky.action


class BlueskyUtil {

    /**
     * {@inheritDoc}
     */
    fun commentLengthLevel(text: String): Float {
        return ((text.length.toFloat()) / 300f)
    }
}
