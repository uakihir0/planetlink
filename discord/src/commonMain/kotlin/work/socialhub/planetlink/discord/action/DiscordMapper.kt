package work.socialhub.planetlink.discord.action

import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.discord.model.DiscordChannel
import work.socialhub.planetlink.discord.model.DiscordComment
import work.socialhub.planetlink.discord.model.DiscordPaging
import work.socialhub.planetlink.discord.model.DiscordThread
import work.socialhub.planetlink.discord.model.DiscordUser
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedString
import kotlin.time.Instant
import work.socialhub.kdiscord.entity.Attachment
import work.socialhub.kdiscord.entity.Channel as DcChannel
import work.socialhub.kdiscord.entity.Message
import work.socialhub.kdiscord.entity.Reaction as DcReaction
import work.socialhub.kdiscord.entity.User as DcUser

/** Discord エンティティのマッピング */
object DiscordMapper {

    // ---------------------------------------------------------------- //
    // User
    // ---------------------------------------------------------------- //

    fun user(
        user: DcUser,
        service: Service,
    ): DiscordUser {
        return DiscordUser(service).apply {
            id = ID(user.id ?: "")
            name = user.globalName ?: user.username ?: (user.id ?: "")
            username = user.username
            discriminator = user.discriminator
            isBot = user.bot ?: false
            user.avatar?.let { avatar ->
                user.id?.let { uid ->
                    iconImageUrl = "https://cdn.discordapp.com/avatars/$uid/$avatar.png"
                }
            }
        }
    }

    // ---------------------------------------------------------------- //
    // Comment (Message)
    // ---------------------------------------------------------------- //

    fun comment(
        message: Message,
        userMe: User?,
        service: Service,
    ): DiscordComment {
        return DiscordComment(service).apply {
            id = ID(message.id ?: "")
            channelId = message.channelId
            guildId = message.guildId
            createAt = parseTimestamp(message.timestamp)
            user = message.author?.let { user(it, service) }
            text = AttributedString.plain(message.content ?: "")
            directMessage = (message.guildId == null)
            medias = medias(message)
            reactions = reactions(message.reactions, userMe)
        }
    }

    fun timeLine(
        messages: List<Message>,
        userMe: User?,
        service: Service,
        paging: Paging?,
    ): Pageable<Comment> {
        val model = Pageable<Comment>()
        model.entities = messages
            .map { comment(it, userMe, service) }
            .sortedByDescending { it.createAt }
        model.paging = DiscordPaging.fromPaging(paging)
        return model
    }

    // ---------------------------------------------------------------- //
    // Channel / Thread
    // ---------------------------------------------------------------- //

    fun channel(
        channel: DcChannel,
        service: Service,
    ): DiscordChannel {
        return DiscordChannel(service).apply {
            id = ID(channel.id ?: "")
            name = channel.name
            description = channel.topic
            topic = channel.topic
            guildId = channel.guildId
            type = channel.type
            position = channel.position
            isPublic = (channel.type == 0) // GUILD_TEXT
        }
    }

    fun channels(
        channels: List<DcChannel>,
        service: Service,
        paging: Paging?,
    ): Pageable<Channel> {
        val model = Pageable<Channel>()
        model.entities = channels.map { channel(it, service) }
        model.paging = DiscordPaging.fromPaging(paging)
        return model
    }

    fun thread(
        channel: DcChannel,
        service: Service,
    ): DiscordThread {
        return DiscordThread(service).apply {
            id = ID(channel.id ?: "")
            channelId = channel.id
            description = channel.name
                ?: channel.recipients?.mapNotNull { it.globalName ?: it.username }?.joinToString(", ")
            users = channel.recipients?.map { user(it, service) }
        }
    }

    fun threads(
        channels: List<DcChannel>,
        service: Service,
        paging: Paging?,
    ): Pageable<Thread> {
        val model = Pageable<Thread>()
        model.entities = channels.map { thread(it, service) }
        model.paging = DiscordPaging.fromPaging(paging)
        return model
    }

    fun users(
        users: List<DcUser>,
        service: Service,
        paging: Paging?,
    ): Pageable<User> {
        val model = Pageable<User>()
        model.entities = users.map { user(it, service) }
        model.paging = DiscordPaging.fromPaging(paging)
        return model
    }

    // ---------------------------------------------------------------- //
    // Reaction / Media
    // ---------------------------------------------------------------- //

    fun reactions(
        reactions: Array<DcReaction>?,
        userMe: User?,
    ): List<Reaction> {
        return reactions?.map { reaction ->
            Reaction().also {
                val emoji = reaction.emoji
                it.name = emoji?.name
                it.count = reaction.count
                it.reacting = reaction.me ?: false
                // Custom emoji: render the CDN image url.
                if (emoji?.id != null) {
                    val ext = if (emoji.animated == true) "gif" else "png"
                    it.iconUrl = "https://cdn.discordapp.com/emojis/${emoji.id}.$ext"
                } else {
                    it.emoji = emoji?.name
                }
            }
        } ?: emptyList()
    }

    fun medias(
        message: Message,
    ): List<Media> {
        return message.attachments?.map { media(it) } ?: emptyList()
    }

    fun media(
        attachment: Attachment,
    ): Media {
        return Media().also {
            it.sourceUrl = attachment.url
            it.previewUrl = attachment.proxyUrl ?: attachment.url
            it.type = when {
                attachment.contentType?.startsWith("image/") == true -> MediaType.Image
                attachment.contentType?.startsWith("video/") == true -> MediaType.Movie
                else -> MediaType.File
            }
        }
    }

    // ---------------------------------------------------------------- //
    // Utility
    // ---------------------------------------------------------------- //

    /** Discord timestamps are ISO8601 strings. */
    fun parseTimestamp(timestamp: String?): Instant? {
        if (timestamp.isNullOrEmpty()) return null
        return try {
            Instant.parse(timestamp)
        } catch (e: Exception) {
            null
        }
    }
}
