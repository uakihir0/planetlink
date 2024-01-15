package work.socialhub.planetlink.bluesky.action

import net.socialhub.core.utils.ServiceUtil

class BlueskyUtil : ServiceUtil {
    /**
     * {@inheritDoc}
     */
    fun getCommentLengthLevel(text: String): Float {
        return ((text.length.toFloat()) / 300f)
    }
}
