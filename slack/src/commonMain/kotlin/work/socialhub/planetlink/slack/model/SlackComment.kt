package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm

class SlackComment(
    service: Service
) : Comment(service) {

    var channelId: String? = null
    var threadTs: String? = null
    var replyCount: Int = 0

    private var _reactions: MutableList<Reaction>? = null

    override var webUrl: String
        get() {
            val ch = channelId ?: return ""
            val ts = id?.value<String>()?.replace(".", "") ?: return ""
            return "https://app.slack.com/archives/$ch/p$ts"
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
        if (_reactions == null) {
            _reactions = mutableListOf(reaction)
            return
        }

        val exist = _reactions!!.find { it.name == reaction.name }
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

        _reactions!!.add(reaction)
    }

    override val replyForm: CommentForm
        get() = CommentForm().also {
            it.addParam(CHANNEL_KEY, channelId ?: "")
            it.isMessage(directMessage)
            it.replyId(id as? ID)
        }

    companion object {
        const val CHANNEL_KEY = "channel"
    }
}
