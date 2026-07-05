package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.request.CommentForm
import kotlin.js.JsExport

/** Discord コメント (メッセージ) モデル */
@JsExport
class DiscordComment(
    service: Service
) : Comment(service) {

    /** Channel id the message belongs to (snowflake). */
    var channelId: String? = null

    /** Guild id the message belongs to (snowflake); null for DMs. */
    var guildId: String? = null

    private var _reactions: MutableList<Reaction>? = null

    override var webUrl: String
        get() {
            val ch = channelId ?: return ""
            val msg = id?.value<String>() ?: return ""
            val guild = guildId ?: "@me"
            return "https://discord.com/channels/$guild/$ch/$msg"
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

    /** {@inheritDoc} */
    override val replyForm: CommentForm
        get() = CommentForm().also {
            it.addParam(CHANNEL_KEY, channelId ?: "")
            it.isMessage(directMessage)
            it.replyId(id)
        }

    companion object {
        const val CHANNEL_KEY = "channel"
    }
}
