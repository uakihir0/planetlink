package work.socialhub.planetlink.nostr.model

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm
import kotlin.js.JsExport

@JsExport
class NostrComment(
    service: Service
) : Comment(service) {

    var eventId: String? = null
    var replyCount: Int = 0
    var likeCount: Int = 0
    var repostCount: Int = 0
    var contentWarning: String? = null
    var channelId: String? = null

    private var _reactions: MutableList<Reaction>? = null

    override var webUrl: String
        get() {
            val noteId = eventId ?: return ""
            return "https://snort.social/e/$noteId"
        }
        set(_) {}

    override var reactions: List<Reaction>
        get() {
            val base = super.reactions.toMutableList()
            _reactions?.let { base.addAll(it) }
            return base
        }
        set(value) {
            _reactions = value.toMutableList()
        }

    override fun applyReaction(reaction: Reaction) {
        val reactions = _reactions ?: run {
            _reactions = mutableListOf(reaction)
            return
        }

        val exist = reactions.find { it.name == reaction.name }
        if (exist != null) {
            if (reaction.reacting && !exist.reacting) {
                exist.count = (exist.count ?: 0) + 1
                exist.reacting = true
            }
            if (!reaction.reacting && exist.reacting) {
                exist.count = maxOf((exist.count ?: 0) - 1, 0)
                exist.reacting = false
            }
            return
        }

        reactions.add(reaction)
    }

    override val replyForm: CommentForm
        get() = CommentForm().also {
            it.replyId(id)
        }

    companion object {
        const val EVENT_ID_KEY = "eventId"
        const val AUTHOR_PUBKEY_KEY = "authorPubkey"
    }
}
