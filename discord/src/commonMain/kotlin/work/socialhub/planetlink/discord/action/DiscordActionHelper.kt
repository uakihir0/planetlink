package work.socialhub.planetlink.discord.action

import work.socialhub.kdiscord.api.request.messages.MessagesCreateRequest
import work.socialhub.kdiscord.api.request.messages.MessagesListRequest
import work.socialhub.kdiscord.entity.share.FileContent
import work.socialhub.kdiscord.stream.DiscordStreamFactory
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.discord.model.DiscordComment
import work.socialhub.planetlink.discord.model.DiscordIdentify
import work.socialhub.planetlink.discord.model.DiscordPaging
import work.socialhub.planetlink.discord.model.DiscordStream
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Stream
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm

/**
 * Internal helper to avoid the Kotlin/JS yield* bug.
 * The JS compiler breaks when a @JsExport suspend function calls another
 * suspend function on the same class. Moving shared suspend logic here (a
 * separate, non-exported class) sidesteps the issue — same pattern as the
 * Slack adapter's SlackActionHelper.
 */
internal class DiscordActionHelper(
    private val action: DiscordAction,
) {
    private val auth get() = action.auth
    private val service get() = action.account.service

    // ---------------------------------------------------------------- //
    // User
    // ---------------------------------------------------------------- //

    suspend fun fetchUserMe(): User {
        return action.proceed {
            val response = auth.accessor.discord.users().getMe()
            val user = DiscordMapper.user(response.data, service)
            action.cacheMe(user)
            user
        }
    }

    suspend fun getUser(id: Identify): User {
        return action.proceed {
            val response = auth.accessor.discord.users().getUser(id.id!!.value())
            DiscordMapper.user(response.data, service)
        }
    }

    // ---------------------------------------------------------------- //
    // Timeline (channel messages)
    // ---------------------------------------------------------------- //

    suspend fun getChannelTimeLine(channelId: String, paging: Paging): Pageable<Comment> {
        val userMe = action.userMeWithCache()
        return action.proceed {
            val dp = DiscordPaging.fromPaging(paging)
            val response = auth.accessor.discord.messages().list(
                MessagesListRequest(channelId).also {
                    it.limit = paging.count ?: 50
                    it.before = dp.before
                    it.after = dp.after
                }
            )
            DiscordMapper.timeLine(response.data.toList(), userMe, service, paging)
        }
    }

    // ---------------------------------------------------------------- //
    // Comment (message) CRUD
    // ---------------------------------------------------------------- //

    suspend fun getComment(id: Identify): Comment {
        val channelId = getChannelId(id)
        val userMe = action.userMeWithCache()
        return action.proceed {
            val response = auth.accessor.discord.messages().get(channelId, id.id!!.value())
            DiscordMapper.comment(response.data, userMe, service)
        }
    }

    suspend fun postComment(req: CommentForm) {
        val channelId = getChannelIdFromForm(req)
        action.proceedUnit {
            auth.accessor.discord.messages().create(
                MessagesCreateRequest(channelId).also {
                    it.content = req.text
                    it.replyMessageId = req.replyId?.value<String>()
                    if (req.images.isNotEmpty()) {
                        it.files = req.images.map { image ->
                            FileContent(filename = image.name, bytes = image.data)
                                .also { fc -> fc.description = image.description }
                        }.toTypedArray()
                    }
                }
            )
        }
    }

    suspend fun deleteComment(id: Identify) {
        val channelId = getChannelId(id)
        action.proceedUnit {
            auth.accessor.discord.messages().delete(channelId, id.id!!.value())
        }
    }

    // ---------------------------------------------------------------- //
    // Reactions
    // ---------------------------------------------------------------- //

    suspend fun likeComment(id: Identify) {
        reactionComment(id, DEFAULT_LIKE_EMOJI)
    }

    suspend fun unlikeComment(id: Identify) {
        unreactionComment(id, DEFAULT_LIKE_EMOJI)
    }

    suspend fun reactionComment(id: Identify, reaction: String) {
        val channelId = getChannelId(id)
        action.proceedUnit {
            auth.accessor.discord.reactions().createReaction(channelId, id.id!!.value(), reaction)
        }
    }

    suspend fun unreactionComment(id: Identify, reaction: String) {
        val channelId = getChannelId(id)
        action.proceedUnit {
            auth.accessor.discord.reactions().deleteOwnReaction(channelId, id.id!!.value(), reaction)
        }
    }

    // ---------------------------------------------------------------- //
    // Channels / Guilds
    // ---------------------------------------------------------------- //

    suspend fun channels(guildId: String, paging: Paging): Pageable<Channel> {
        return action.proceed {
            val response = auth.accessor.discord.guilds().listGuildChannels(guildId)
            DiscordMapper.channels(response.data.toList(), service, paging)
        }
    }

    // ---------------------------------------------------------------- //
    // Message thread (DM channels)
    // ---------------------------------------------------------------- //

    suspend fun messageThread(paging: Paging): Pageable<Thread> {
        return action.proceed {
            val response = auth.accessor.discord.channels().listDmChannels()
            DiscordMapper.threads(response.data.toList(), service, paging)
        }
    }

    // ---------------------------------------------------------------- //
    // Streaming
    // ---------------------------------------------------------------- //

    fun homeTimeLineStream(callback: EventCallback): Stream {
        val kStream = auth.apiHost
            ?.let { DiscordStreamFactory.instance(auth.accessor.token, it) }
            ?: DiscordStreamFactory.instance(auth.accessor.token)

        kStream.addEventListener(
            DiscordStreamListenerImpl(callback, service)
        )
        return DiscordStream(kStream)
    }

    // ---------------------------------------------------------------- //
    // Channel id resolution
    // ---------------------------------------------------------------- //

    private fun getChannelId(id: Identify): String {
        return when (id) {
            is DiscordComment -> id.channelId
            is DiscordIdentify -> id.channelId
            else -> null
        } ?: throw SocialHubException(
            "Channel id is required. Pass a DiscordComment or DiscordIdentify carrying channelId."
        )
    }

    private fun getChannelIdFromForm(req: CommentForm): String {
        val channelId = req.params[DiscordComment.CHANNEL_KEY] as? String
        return channelId?.takeIf { it.isNotEmpty() }
            ?: throw SocialHubException(
                "Channel id is required in the comment form (params[\"channel\"])."
            )
    }

    companion object {
        /** Default emoji used for like/unlike (Discord has no dedicated "like"). */
        const val DEFAULT_LIKE_EMOJI = "👍" // 👍
    }
}
