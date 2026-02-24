package work.socialhub.planetlink.slack.action

import kotlinx.datetime.Instant
import work.socialhub.kslack.api.methods.response.bots.BotsInfoResponse
import work.socialhub.kslack.api.methods.response.conversations.ConversationsListResponse
import work.socialhub.kslack.api.methods.response.emoji.EmojiListResponse
import work.socialhub.kslack.api.methods.response.team.TeamInfoResponse
import work.socialhub.kslack.api.methods.response.users.UsersInfoResponse
import work.socialhub.kslack.entity.Conversation
import work.socialhub.kslack.entity.message.Message
import work.socialhub.kslack.entity.file.File
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.define.emoji.EmojiCategoryType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.common.AttributedItem
import work.socialhub.planetlink.model.common.AttributedKind
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.slack.model.*

object SlackMapper {

    fun user(
        user: work.socialhub.kslack.entity.user.User,
        team: SlackTeam?,
        service: Service
    ): SlackUser {
        return SlackUser(service).apply {
            isBot = user.isBot

            id = ID(user.id ?: "")
            name = user.profile?.displayName ?: user.realName ?: user.name ?: ""
            screenName = user.name ?: ""

            val profile = user.profile
            if (profile != null) {
                iconImageUrl = profile.image512 ?: profile.image192 ?: profile.image72

                if (!profile.title.isNullOrEmpty()) {
                    description = AttributedString.plain(profile.title)
                }
                if (!profile.displayName.isNullOrEmpty()) {
                    displayName = profile.displayName
                }
            }

            this.team = team
        }
    }

    fun user(
        response: UsersInfoResponse,
        team: SlackTeam?,
        service: Service
    ): SlackUser? {
        return response.user?.let { user(it, team, service) }
    }

    fun bots(
        response: BotsInfoResponse,
        service: Service
    ): SlackUser? {
        val bot = response.bot ?: return null
        return SlackUser(service).apply {
            isBot = true
            id = ID(bot.id ?: "")
            name = bot.name ?: ""

            bot.icons?.let { icons ->
                iconImageUrl = icons.image72 ?: icons.image48 ?: icons.image36
            }
        }
    }

    fun comment(
        message: Message,
        user: User?,
        userMe: User?,
        emojis: List<Emoji>?,
        channel: String,
        service: Service,
        token: String? = null,
    ): SlackComment {
        return SlackComment(service).apply {
            id = ID(message.ts ?: "")
            threadTs = message.threadTs
            createAt = getDateFromTimeStamp(message.ts)
            this.user = user

            text = AttributedString.plain(message.text ?: "")

            this.channelId = channel
            if (message.channel != null) {
                this.channelId = message.channel
            }

            reactions = reactions(message, userMe, emojis)

            replyCount = message.replyCount ?: 0

            medias = medias(message, token)
        }
    }

    fun medias(
        message: Message,
        token: String? = null,
    ): List<Media> {
        val medias = mutableListOf<Media>()

        message.file?.let { medias.add(media(it, token)) }
        message.files?.forEach { medias.add(media(it, token)) }

        return medias
    }

    fun media(
        file: File,
        token: String? = null,
    ): Media {
        return slackMedia(token).apply {
            sourceUrl = file.urlPrivate
            previewUrl = file.thumb360 ?: file.urlPrivate

            type = when {
                file.mimetype?.startsWith("image") == true -> MediaType.Image
                file.mimetype?.startsWith("video") == true -> MediaType.Movie
                file.mimetype?.startsWith("audio") == true -> MediaType.Other
                else -> MediaType.File
            }
        }
    }

    fun emojis(
        response: EmojiListResponse
    ): List<Emoji> {
        val result = mutableListOf<Emoji>()
        val alias = mutableMapOf<String, String>()

        response.emoji?.forEach { (key, value) ->
            if (value.startsWith("alias")) {
                val parts = value.split(":")
                if (parts.size > 1) {
                    alias[key] = parts[1]
                }
            } else {
                val emoji = Emoji()
                emoji.addShortCode(key)
                emoji.imageUrl = value
                emoji.category = EmojiCategoryType.Custom.code
                result.add(emoji)
            }
        }

        alias.forEach { (key, value) ->
            result.find { it.shortCodes.getOrNull(0) == value }?.let {
                it.addShortCode(key)
            }
        }

        return result
    }

    fun reactions(
        message: Message,
        userMe: User?,
        emojis: List<Emoji>?
    ): List<Reaction> {
        val models = mutableListOf<Reaction>()

        message.reactions?.forEach { reaction ->
            val model = Reaction()
            model.count = reaction.count ?: 0
            model.name = reaction.name ?: ""

            userMe?.id?.value<String>()?.let { uid ->
                model.reacting = reaction.users?.contains(uid) == true
            }

            emojis?.let { list ->
                list.find { it.shortCodes.getOrNull(0) == reaction.name }?.let { emoji ->
                    model.iconUrl = emoji.imageUrl
                    model.emoji = emoji.emoji
                }
            }

            models.add(model)
        }

        message.replyCount?.let {
            val model = Reaction()
            model.count = it
            model.name = "reply"
            models.add(model)
        }

        return models
    }

    fun timeLine(
        messages: List<Message>,
        userMap: Map<String, User>,
        botMap: Map<String, User>,
        userMe: User?,
        emojis: List<Emoji>?,
        channel: String,
        service: Service,
        paging: Paging?,
        token: String? = null,
    ): Pageable<Comment> {
        val model = Pageable<Comment>()
        model.entities = messages.mapNotNull { msg ->
            val user = if (msg.subtype == "bot_message") {
                botMap[msg.botId]
            } else {
                userMap[msg.user]
            }
            comment(msg, user, userMe, emojis, channel, service, token)
        }.sortedByDescending { it.createAt }

        model.paging = SlackPaging.fromPaging(paging)
        return model
    }

    fun channels(
        response: ConversationsListResponse,
        service: Service
    ): Pageable<Channel> {
        val model = Pageable<Channel>()
        val entities = response.channels?.map { c ->
            channel(c, service)
        } ?: emptyList()

        val pg = SlackPaging()
        pg.count = entities.size

        model.entities = entities
        model.paging = pg
        return model
    }

    fun channel(
        c: Conversation,
        service: Service
    ): SlackChannel {
        return SlackChannel(service).apply {
            id = ID(c.id ?: "")
            name = c.name ?: ""
            description = c.purpose?.value ?: ""
            createAt = c.created?.let { Instant.fromEpochSeconds(it.toLong(), 0) }

            isChannel = c.isChannel ?: false
            isGroup = c.isGroup ?: false
            isIm = c.isIm ?: false
            isMpim = c.isMpim ?: false
            isPrivate = c.isPrivate ?: false
            isArchived = c.isArchived ?: false
            isGeneral = c.isGeneral ?: false
            isPublic = !isPrivate

            creator = c.creator
            topic = c.topic?.value
            purpose = c.purpose?.value
            numMembers = c.numOfMembers
        }
    }

    fun threads(
        response: ConversationsListResponse,
        memberMap: Map<String, List<String>>,
        historyMap: Map<String, Instant>,
        accountMap: Map<String, User>,
        service: Service
    ): List<Thread> {
        return response.channels?.mapNotNull { c ->
            if (c.isArchived == true) return@mapNotNull null

            Thread(service).apply {
                id = ID(c.id ?: "")
                users = memberMap[c.id]
                    ?.mapNotNull { accountMap[it] }
                    ?: emptyList()

                lastUpdate = historyMap[c.id]
                    ?: c.created?.let { Instant.fromEpochSeconds(it.toLong(), 0) }
            }
        } ?: emptyList()
    }

    fun team(
        response: TeamInfoResponse
    ): SlackTeam? {
        if (!response.isOk) return null

        return response.team?.let { t ->
            SlackTeam().apply {
                id = t.id
                name = t.name
                domain = t.domain
                iconImageUrl = t.icon?.image132
            }
        }
    }

    fun getDateFromTimeStamp(ts: String?): Instant? {
        if (ts.isNullOrEmpty()) return null
        val unixTime = ts.split(".")[0].toLongOrNull() ?: return null
        return Instant.fromEpochSeconds(unixTime, 0)
    }

    fun getReplayUserIds(comments: List<Comment>): List<String> {
        return comments.flatMap { c ->
            c.text?.elements?.filter { it.kind == AttributedKind.ACCOUNT }
                ?.mapNotNull { it.displayText?.substring(1) }
                ?.filter { n -> n != "here" && n != "all" && n != "channel" }
                ?: emptyList()
        }.distinct()
    }

    fun setMentionName(comments: List<Comment>, userMap: Map<String, User>) {
        comments.forEach { c ->
            c.text?.elements?.filter { it.kind == AttributedKind.ACCOUNT }
                ?.forEach { elem ->
                    val userId = elem.displayText?.substring(1) ?: return@forEach
                    if (elem is AttributedItem && userMap.containsKey(userId)) {
                        elem.displayText = "@${userMap[userId]!!.name}"
                        elem.expandedText = userId
                    }
                }
        }
    }
}
