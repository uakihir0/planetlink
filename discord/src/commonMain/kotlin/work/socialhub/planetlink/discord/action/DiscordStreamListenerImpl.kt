package work.socialhub.planetlink.discord.action

import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.kdiscord.entity.Message
import work.socialhub.kdiscord.entity.gateway.event.MessageDeleteEvent
import work.socialhub.kdiscord.stream.DiscordStreamListener
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.DeleteCommentCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.event.IdentifyEvent
import work.socialhub.planetlink.utils.ExceptionHandler

/**
 * Translates kdiscord Gateway events into planetlink [EventCallback] invocations.
 */
internal class DiscordStreamListenerImpl(
    private val callback: EventCallback,
    private val service: Service,
) : DiscordStreamListener {

    override fun onMessageCreate(message: Message) {
        if (callback is UpdateCommentCallback) {
            val comment = DiscordMapper.comment(message, null, service)
            callback.onUpdate(CommentEvent(comment))
        }
    }

    override fun onMessageUpdate(message: Message) {
        if (callback is UpdateCommentCallback) {
            val comment = DiscordMapper.comment(message, null, service)
            callback.onUpdate(CommentEvent(comment))
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (callback is DeleteCommentCallback) {
            event.id?.let { callback.onDelete(IdentifyEvent(it)) }
        }
    }

    override fun onOpen() {
        if (callback is ConnectCallback) {
            callback.onConnect()
        }
    }

    override fun onClose() {
        if (callback is DisconnectCallback) {
            callback.onDisconnect()
        }
    }

    override fun onError(error: Exception) {
        if (callback is ErrorCallback) {
            // Classify through ExceptionHandler so the exception carries the
            // Discord serviceType, consistent with the other adapters.
            callback.onError(
                ExceptionHandler.classify(error, ServiceType.Discord)
            )
        }
    }
}
