package work.socialhub.planetlink.discord.action

import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.discord.model.DiscordChannel
import work.socialhub.planetlink.discord.model.DiscordComment
import work.socialhub.planetlink.discord.model.DiscordEmbed
import work.socialhub.planetlink.discord.model.DiscordEmbedAuthor
import work.socialhub.planetlink.discord.model.DiscordEmbedField
import work.socialhub.planetlink.discord.model.DiscordEmbedFooter
import work.socialhub.planetlink.discord.model.DiscordEmbedProvider
import work.socialhub.planetlink.discord.model.DiscordMedia
import work.socialhub.planetlink.discord.model.DiscordMessageComponent
import work.socialhub.planetlink.discord.model.DiscordPaging
import work.socialhub.planetlink.discord.model.DiscordReactionDetails
import work.socialhub.planetlink.discord.model.DiscordSpace
import work.socialhub.planetlink.discord.model.DiscordThread
import work.socialhub.planetlink.discord.model.DiscordUser
import work.socialhub.planetlink.discord.model.DiscordUserPrimaryGuild
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Space
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedElement
import work.socialhub.planetlink.model.common.AttributedItem
import work.socialhub.planetlink.model.common.AttributedKind
import work.socialhub.planetlink.model.common.AttributedString
import kotlin.time.Instant
import work.socialhub.kdiscord.entity.ActionRowComponent
import work.socialhub.kdiscord.entity.Attachment
import work.socialhub.kdiscord.entity.ButtonComponent
import work.socialhub.kdiscord.entity.ChannelSelectComponent
import work.socialhub.kdiscord.entity.CheckboxGroupComponent
import work.socialhub.kdiscord.entity.Channel as DcChannel
import work.socialhub.kdiscord.entity.ContainerComponent
import work.socialhub.kdiscord.entity.Embed as DcEmbed
import work.socialhub.kdiscord.entity.EmbedMedia as DcEmbedMedia
import work.socialhub.kdiscord.entity.FileComponent
import work.socialhub.kdiscord.entity.Guild
import work.socialhub.kdiscord.entity.LabelComponent
import work.socialhub.kdiscord.entity.MediaGalleryComponent
import work.socialhub.kdiscord.entity.MentionableSelectComponent
import work.socialhub.kdiscord.entity.Message
import work.socialhub.kdiscord.entity.MessageComponent
import work.socialhub.kdiscord.entity.RadioGroupComponent
import work.socialhub.kdiscord.entity.Reaction as DcReaction
import work.socialhub.kdiscord.entity.RoleSelectComponent
import work.socialhub.kdiscord.entity.SectionComponent
import work.socialhub.kdiscord.entity.StringSelectComponent
import work.socialhub.kdiscord.entity.TextDisplayComponent
import work.socialhub.kdiscord.entity.TextInputComponent
import work.socialhub.kdiscord.entity.ThumbnailComponent
import work.socialhub.kdiscord.entity.UnfurledMediaItem
import work.socialhub.kdiscord.entity.UserSelectComponent
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
            flags = user.flags
            publicFlags = user.publicFlags
            clan = user.clan?.let { primaryGuild(it) }
            primaryGuild = user.primaryGuild?.let { primaryGuild(it) }
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
            editedTimestamp = message.editedTimestamp
            user = message.author?.let { user(it, service) }
            text = attributedText(message)
            directMessage = (message.guildId == null)
            medias = medias(message)
            reactions = reactions(message.reactions, userMe)
            tts = message.tts ?: false
            mentionEveryone = message.mentionEveryone ?: false
            mentionRoleIds = message.mentionRoles?.toList() ?: emptyList()
            pinned = message.pinned ?: false
            webhookId = message.webhookId
            messageType = message.type
            messageFlags = message.flags
            embeds = message.embeds?.map { embed(it) } ?: emptyList()
            components = message.components?.map { component(it) } ?: emptyList()
            reactionDetails = reactionDetails(message.reactions)
        }
    }

    private fun attributedText(
        message: Message,
    ): AttributedString {
        val content = displayText(message)
        val mentionsById = message.mentions
            .orEmpty()
            .mapNotNull { user -> user.id?.let { it to user } }
            .toMap()

        if (mentionsById.isEmpty()) return AttributedString.plain(content)

        data class MentionInfo(val displayName: String, val id: String)
        val resolved = mutableListOf<MentionInfo>()
        val markerRegex = "\u0000(\\d+)\u0000".toRegex()

        val cleaned = buildString {
            var last = 0
            for (match in USER_MENTION_REGEX.findAll(content)) {
                val mentionId = match.groupValues[1]
                val mention = mentionsById[mentionId]
                val displayName = mention?.globalName?.takeIf { it.isNotBlank() }
                    ?: mention?.username?.takeIf { it.isNotBlank() }
                if (displayName != null) {
                    append(content.substring(last, match.range.first))
                    append("\u0000${resolved.size}\u0000")
                    resolved.add(MentionInfo(displayName, mentionId))
                    last = match.range.last + 1
                }
            }
            append(content.substring(last))
        }

        if (resolved.isEmpty()) return AttributedString.plain(content)

        val parsed = AttributedString.plain(cleaned)
        val newElements = mutableListOf<AttributedElement>()

        for (element in parsed.elements) {
            if (element.kind == AttributedKind.PLAIN) {
                val text = element.displayText
                var readIndex = 0
                var changed = false

                for (match in markerRegex.findAll(text)) {
                    val index = match.groupValues[1].toInt()
                    val info = resolved[index]

                    val before = text.substring(readIndex, match.range.first)
                    if (before.isNotEmpty()) {
                        AttributedItem().also {
                            it.kind = AttributedKind.PLAIN
                            it.displayText = before
                            newElements.add(it)
                        }
                    }
                    AttributedItem().also {
                        it.kind = AttributedKind.ACCOUNT
                        it.displayText = "@${info.displayName}"
                        it.expandedText = info.id
                        newElements.add(it)
                    }
                    readIndex = match.range.last + 1
                    changed = true
                }

                if (changed) {
                    val after = text.substring(readIndex)
                    if (after.isNotEmpty()) {
                        AttributedItem().also {
                            it.kind = AttributedKind.PLAIN
                            it.displayText = after
                            newElements.add(it)
                        }
                    }
                } else {
                    newElements.add(element)
                }
            } else {
                newElements.add(element)
            }
        }

        return AttributedString.elements(newElements)
    }

    private fun displayText(message: Message): String {
        val parts = mutableListOf<String>()
        message.content?.takeIf { it.isNotBlank() }?.let(parts::add)

        message.embeds.orEmpty().forEach { embed ->
            embed.author?.name?.takeIf { it.isNotBlank() }?.let(parts::add)
            embed.title?.takeIf { it.isNotBlank() }?.let(parts::add)
            embed.description?.takeIf { it.isNotBlank() }?.let(parts::add)
            embed.fields.orEmpty().forEach { field ->
                listOfNotNull(
                    field.name?.takeIf { it.isNotBlank() },
                    field.value?.takeIf { it.isNotBlank() },
                ).joinToString("\n")
                    .takeIf { it.isNotBlank() }
                    ?.let(parts::add)
            }
            embed.footer?.text?.takeIf { it.isNotBlank() }?.let(parts::add)
            embed.provider?.name?.takeIf { it.isNotBlank() }?.let(parts::add)
        }

        message.components.orEmpty().forEach { component ->
            componentTextParts(component, parts)
        }
        return parts.joinToString("\n\n")
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
    // Space (Guild)
    // ---------------------------------------------------------------- //

    fun space(
        guild: Guild,
        service: Service,
    ): DiscordSpace {
        return DiscordSpace(service).apply {
            id = ID(guild.id ?: "")
            name = guild.name
            description = guild.description
            owner = guild.owner
            approximateMemberCount = guild.approximateMemberCount
            guild.icon?.let { icon ->
                guild.id?.let { gid ->
                    iconUrl = "https://cdn.discordapp.com/icons/$gid/$icon.png"
                }
            }
        }
    }

    fun spaces(
        guilds: List<Guild>,
        service: Service,
        paging: Paging?,
    ): Pageable<Space> {
        val model = Pageable<Space>()
        // GET /users/@me/guilds returns guilds ascending by id (oldest first),
        // but DiscordPaging.newPage()/pastPage() expect entities newest-first
        // (first = newest -> after cursor, last = oldest -> before cursor), same
        // as timeLine(). Reverse so nextPage()/prevPage() advance without overlap.
        model.entities = guilds
            .map { space(it, service) }
            .reversed()
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
    // Rich message
    // ---------------------------------------------------------------- //

    private fun primaryGuild(
        source: work.socialhub.kdiscord.entity.UserPrimaryGuild,
    ): DiscordUserPrimaryGuild {
        return DiscordUserPrimaryGuild().apply {
            identityGuildId = source.identityGuildId
            identityEnabled = source.identityEnabled ?: false
            tag = source.tag
            badge = source.badge
        }
    }

    fun embed(source: DcEmbed): DiscordEmbed {
        return DiscordEmbed().apply {
            title = source.title
            type = source.type
            description = source.description
            url = source.url
            timestamp = source.timestamp
            color = source.color
            footer = source.footer?.let {
                DiscordEmbedFooter().apply {
                    text = it.text
                    iconUrl = it.iconUrl
                    proxyIconUrl = it.proxyIconUrl
                }
            }
            image = source.image?.let { embedMedia(it, MediaType.Image) }
            thumbnail = source.thumbnail?.let { embedMedia(it, MediaType.Image) }
            video = source.video?.let { embedMedia(it, MediaType.Movie) }
            provider = source.provider?.let {
                DiscordEmbedProvider().apply {
                    name = it.name
                    url = it.url
                }
            }
            author = source.author?.let {
                DiscordEmbedAuthor().apply {
                    name = it.name
                    url = it.url
                    iconUrl = it.iconUrl
                    proxyIconUrl = it.proxyIconUrl
                }
            }
            fields = source.fields.orEmpty().map {
                DiscordEmbedField().apply {
                    name = it.name
                    value = AttributedString.plain(it.value)
                    inline = it.inline ?: false
                }
            }
            contentScanVersion = source.contentScanVersion
        }
    }

    fun component(source: MessageComponent): DiscordMessageComponent {
        val ownText = componentOwnText(source).joinToString("\n")
        return DiscordMessageComponent().apply {
            type = source.type
            id = source.id
            text = ownText.takeIf { it.isNotBlank() }?.let(AttributedString::plain)
            url = (source as? ButtonComponent)?.url
            disabled = componentDisabled(source)
            spoiler = componentSpoiler(source)
            medias = componentOwnMedias(source)
            children = componentChildren(source).map { component(it) }
        }
    }

    private fun componentTextParts(
        component: MessageComponent,
        destination: MutableList<String>,
    ) {
        destination.addAll(componentOwnText(component).filter { it.isNotBlank() })
        componentChildren(component).forEach {
            componentTextParts(it, destination)
        }
    }

    private fun componentOwnText(component: MessageComponent): List<String> {
        return when (component) {
            is ButtonComponent -> listOfNotNull(component.label)
            is StringSelectComponent -> listOfNotNull(component.placeholder) +
                component.options.orEmpty().flatMap {
                    listOfNotNull(it.label, it.description)
                }
            is TextInputComponent -> listOfNotNull(component.label, component.value)
            is UserSelectComponent -> listOfNotNull(component.placeholder)
            is RoleSelectComponent -> listOfNotNull(component.placeholder)
            is MentionableSelectComponent -> listOfNotNull(component.placeholder)
            is ChannelSelectComponent -> listOfNotNull(component.placeholder)
            is TextDisplayComponent -> listOfNotNull(component.content)
            is ThumbnailComponent -> listOfNotNull(component.description)
            is MediaGalleryComponent -> component.items.orEmpty()
                .mapNotNull { it.description }
            is FileComponent -> listOfNotNull(component.name)
            is LabelComponent -> listOfNotNull(component.label, component.description)
            is RadioGroupComponent -> component.options.orEmpty().flatMap {
                listOfNotNull(it.label, it.description)
            }
            is CheckboxGroupComponent -> component.options.orEmpty().flatMap {
                listOfNotNull(it.label, it.description)
            }
            else -> emptyList()
        }
    }

    private fun componentChildren(component: MessageComponent): List<MessageComponent> {
        return when (component) {
            is ActionRowComponent -> component.components.orEmpty().toList()
            is SectionComponent -> component.components.orEmpty().toList() +
                listOfNotNull(component.accessory)
            is ContainerComponent -> component.components.orEmpty().toList()
            is LabelComponent -> listOfNotNull(component.component)
            else -> emptyList()
        }
    }

    private fun componentDisabled(component: MessageComponent): Boolean {
        return when (component) {
            is ButtonComponent -> component.disabled
            is StringSelectComponent -> component.disabled
            is UserSelectComponent -> component.disabled
            is RoleSelectComponent -> component.disabled
            is MentionableSelectComponent -> component.disabled
            is ChannelSelectComponent -> component.disabled
            else -> null
        } ?: false
    }

    private fun componentSpoiler(component: MessageComponent): Boolean {
        return when (component) {
            is ThumbnailComponent -> component.spoiler
            is FileComponent -> component.spoiler
            is ContainerComponent -> component.spoiler
            else -> null
        } ?: false
    }

    private fun componentOwnMedias(component: MessageComponent): List<Media> {
        return when (component) {
            is ThumbnailComponent -> listOfNotNull(
                component.media?.let {
                    componentMedia(
                        source = it,
                        fallbackType = MediaType.Image,
                        description = component.description,
                        spoiler = component.spoiler ?: false,
                    )
                }
            )
            is MediaGalleryComponent -> component.items.orEmpty().mapNotNull { item ->
                item.media?.let {
                    componentMedia(
                        source = it,
                        fallbackType = MediaType.Image,
                        description = item.description,
                        spoiler = item.spoiler ?: false,
                    )
                }
            }
            is FileComponent -> listOfNotNull(
                component.file?.let {
                    componentMedia(
                        source = it,
                        fallbackType = MediaType.File,
                        description = component.name,
                        spoiler = component.spoiler ?: false,
                    )
                }
            )
            else -> emptyList()
        }
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
                it.reacting = reaction.me == true ||
                    reaction.meBurst == true ||
                    reaction.burstMe == true
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

    fun reactionDetails(
        reactions: Array<DcReaction>?,
    ): List<DiscordReactionDetails> {
        return reactions.orEmpty().map { reaction ->
            DiscordReactionDetails().apply {
                name = reaction.emoji?.name
                emojiId = reaction.emoji?.id
                normalCount = reaction.countDetails?.normal
                burstCount = reaction.countDetails?.burst ?: reaction.burstCount
                burstColors = reaction.burstColors?.toList() ?: emptyList()
                reactingNormally = reaction.me ?: false
                reactingWithBurst = reaction.meBurst == true || reaction.burstMe == true
            }
        }
    }

    fun medias(
        message: Message,
    ): List<Media> {
        val models = mutableListOf<Media>()
        models.addAll(message.attachments.orEmpty().map { media(it) })
        message.embeds.orEmpty().forEach { embed ->
            embed.image?.let { models.add(embedMedia(it, MediaType.Image)) }
            embed.thumbnail?.let { models.add(embedMedia(it, MediaType.Image)) }
            embed.video?.let { models.add(embedMedia(it, MediaType.Movie)) }
        }
        message.components.orEmpty().forEach {
            collectComponentMedias(it, models)
        }
        return models
            .filter { it.sourceUrl != null || it.previewUrl != null }
            .distinctBy { "${it.type}:${it.sourceUrl}:${it.previewUrl}" }
    }

    fun media(
        attachment: Attachment,
    ): DiscordMedia {
        return DiscordMedia().also {
            it.sourceUrl = attachment.url
            it.previewUrl = attachment.proxyUrl ?: attachment.url
            it.type = mediaType(attachment.contentType, MediaType.File)
            it.width = attachment.width
            it.height = attachment.height
            it.description = attachment.description
            it.contentType = attachment.contentType
            it.attachmentId = attachment.id
        }
    }

    private fun embedMedia(
        source: DcEmbedMedia,
        fallbackType: MediaType,
    ): DiscordMedia {
        return DiscordMedia().also {
            it.sourceUrl = source.url
            it.previewUrl = source.proxyUrl ?: source.url
            it.type = mediaType(source.contentType, fallbackType)
            it.width = source.width
            it.height = source.height
            it.contentType = source.contentType
            it.placeholder = source.placeholder
            it.placeholderVersion = source.placeholderVersion
        }
    }

    private fun componentMedia(
        source: UnfurledMediaItem,
        fallbackType: MediaType,
        description: String?,
        spoiler: Boolean,
    ): DiscordMedia {
        return DiscordMedia().also {
            it.sourceUrl = source.url
            it.previewUrl = source.proxyUrl ?: source.url
            it.type = mediaType(source.contentType, fallbackType)
            it.width = source.width
            it.height = source.height
            it.description = description
            it.contentType = source.contentType
            it.placeholder = source.placeholder
            it.placeholderVersion = source.placeholderVersion
            it.spoiler = spoiler
            it.attachmentId = source.attachmentId
        }
    }

    private fun collectComponentMedias(
        component: MessageComponent,
        destination: MutableList<Media>,
    ) {
        destination.addAll(componentOwnMedias(component))
        componentChildren(component).forEach {
            collectComponentMedias(it, destination)
        }
    }

    private fun mediaType(
        contentType: String?,
        fallbackType: MediaType,
    ): MediaType {
        return when {
            contentType?.startsWith("image/") == true -> MediaType.Image
            contentType?.startsWith("video/") == true -> MediaType.Movie
            else -> fallbackType
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

    private val USER_MENTION_REGEX = """<@!?([0-9]+)>""".toRegex()
}
