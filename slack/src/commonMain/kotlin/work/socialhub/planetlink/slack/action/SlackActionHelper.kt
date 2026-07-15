package work.socialhub.planetlink.slack.action

import kotlin.time.Instant
import work.socialhub.kslack.api.methods.SlackApiException
import work.socialhub.kslack.api.methods.request.bots.BotsInfoRequest
import work.socialhub.kslack.api.methods.request.conversations.*
import work.socialhub.kslack.api.methods.request.emoji.EmojiListRequest
import work.socialhub.kslack.api.methods.request.team.TeamInfoRequest
import work.socialhub.kslack.api.methods.request.users.UsersInfoRequest
import work.socialhub.kslack.api.methods.request.users.UsersListRequest
import work.socialhub.kslack.entity.Conversation
import work.socialhub.kslack.entity.ConversationType
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.paging.DatePaging
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.slack.model.*
import work.socialhub.planetlink.utils.ExceptionHandler

/**
 * Internal helper to avoid Kotlin/JS yield* bug.
 * JS compiler breaks when a @JsExport suspend function calls
 * another suspend function on the same class. Moving shared
 * suspend logic here (a separate class) sidesteps the issue.
 */
internal class SlackActionHelper(
    private val action: SlackAction,
) {
    private val auth get() = action.auth
    private val userCache get() = action.userCache
    private val botCache get() = action.botCache
    private val service get() = action.account.service

    var team: SlackTeam? = null
    var generalChannel: String? = null
    var emojisCache: List<Emoji>? = null
    var userMeId: String? = null

    suspend fun loadTeam(): SlackTeam? {
        if (team != null) return team

        return try {
            val response = auth.accessor.slack.team().teamInfo(
                TeamInfoRequest(auth.accessor.token)
            )
            if (response.isOk) {
                team = SlackMapper.team(response)
            }
            team
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadGeneralChannel(): String {
        if (generalChannel == null) {
            proceed {
                val response = auth.accessor.slack.conversations().conversationsList(
                    ConversationsListRequest(
                        token = auth.accessor.token,
                        cursor = null,
                        isExcludeArchived = false,
                        limit = 1000,
                        types = arrayOf(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL)
                    )
                )
                if (response.isOk) {
                    response.channels?.find { it.isGeneral }?.let {
                        generalChannel = it.id
                    }
                }
            }
        }
        return generalChannel ?: throw SocialHubException("General channel not found.")
    }

    suspend fun loadUsersCache() {
        val teamObj = loadTeam()
        val response = proceed {
            auth.accessor.slack.users().usersList(
                UsersListRequest(
                    token = auth.accessor.token,
                    cursor = null,
                    limit = 200,
                    isIncludeLocale = false,
                    isPresence = false
                )
            )
        }

        if (!response.isOk) {
            throw SocialHubException(response.error ?: "Unknown error")
        }

        response.members?.forEach { u ->
            val user = SlackMapper.user(u, teamObj, service)
            user.let { userCache[it.id!!.value<String>()] = it }
        }
    }

    suspend fun getEmojis(): List<Emoji> {
        if (emojisCache != null) return emojisCache!!

        return proceed {
            val response = auth.accessor.slack.emoji().emojiList(
                EmojiListRequest(auth.accessor.token)
            )

            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }

            val emojis = SlackMapper.emojis(response)
            emojisCache = emojis + action.emojis()
            emojisCache!!
        }
    }

    suspend fun getUserWithCache(id: Identify): User {
        val key = id.id!!.value<String>()
        val cached = userCache[key]
        if (cached != null) return cached

        val teamObj = loadTeam()
        return proceed {
            val response = auth.accessor.slack.users().usersInfo(
                UsersInfoRequest(
                    token = auth.accessor.token,
                    user = key,
                    isIncludeLocale = false
                )
            )
            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }
            val user = SlackMapper.user(response, teamObj, service)
            user?.let { userCache[it.id!!.value<String>()] = it }
            user!!
        }
    }

    suspend fun getBotWithCache(id: Identify): User {
        val key = id.id!!.value<String>()
        val cached = botCache[key]
        if (cached != null) return cached

        return proceed {
            val response = auth.accessor.slack.bots().botsInfo(
                BotsInfoRequest(
                    token = auth.accessor.token,
                    bot = key
                )
            )
            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }
            val bot = SlackMapper.bots(response, service)!!
            botCache[bot.id!!.value<String>()] = bot
            bot
        }
    }

    suspend fun getChannelTimeLine(channel: String, paging: Paging): Pageable<Comment> {
        val emojis = getEmojis()
        val userMe = action.userMeWithCache()
        val datePaging = if (paging is DatePaging) paging else null

        val response = proceed {
            auth.accessor.slack.conversations().conversationsHistory(
                ConversationsHistoryRequest(
                    token = auth.accessor.token,
                    channel = channel,
                    cursor = null,
                    oldest = datePaging?.oldest,
                    latest = datePaging?.latest,
                    limit = paging.count ?: 100,
                    isInclusive = datePaging?.inclusive ?: false
                )
            )
        }

        if (!response.isOk) {
            throw SocialHubException(response.error ?: "Unknown error")
        }

        val messages = response.messages?.toList() ?: emptyList()

        val userIds = messages
            .filter { it.subtype != "bot_message" }
            .mapNotNull { it.user }
            .distinct()
        val userMap = userIds.associateWith { getUserWithCache(Identify(service, ID(it))) }

        val botIds = messages
            .filter { it.subtype == "bot_message" }
            .mapNotNull { it.botId }
            .distinct()
        val botMap = botIds.associateWith { getBotWithCache(Identify(service, ID(it))) }

        val pageable = SlackMapper.timeLine(messages, userMap, botMap, userMe, emojis, channel, service, paging, auth.accessor.token)

        val allComments = pageable.entities + pageable.displayableEntities
        SlackMapper.setMentionName(allComments, userMap)

        return pageable
    }

    suspend fun getMessageThread(paging: Paging): Pageable<Thread> {
        val userMe = action.userMeWithCache()
        val pageSize = paging.count

        val response = proceed {
            auth.accessor.slack.conversations().conversationsList(
                ConversationsListRequest(
                    token = auth.accessor.token,
                    cursor = null,
                    isExcludeArchived = true,
                    limit = pageSize?.coerceIn(1, 1000) ?: 1000,
                    types = arrayOf(ConversationType.IM, ConversationType.MPIM)
                )
            )
        }

        if (!response.isOk) {
            throw SocialHubException(response.error ?: "Unknown error")
        }

        val memberMap = mutableMapOf<String, List<String>>()
        val historyMap = mutableMapOf<String, Instant>()
        val userMeId = userMe.id!!.value<String>()

        for (channel in response.channels.orEmpty()) {
            if (!SlackMapper.isVisibleThread(channel, userMeId)) continue
            val channelId = channel.id ?: continue
            try {
                memberMap[channelId] = getThreadMemberIds(channel, userMeId)
            } catch (e: Exception) {
                // Skip channels that fail to resolve members
            }
        }

        val allUserIds = memberMap.values.flatten().distinct()
        val accountMap = allUserIds.mapNotNull { uid ->
            try {
                uid to getUserWithCache(Identify(service, ID(uid)))
            } catch (e: Exception) {
                null // Skip users that fail to resolve
            }
        }.toMap()

        val threads = SlackMapper.threads(response, memberMap, historyMap, accountMap, userMeId, service)
            .sortedByDescending { it.lastUpdate }
            .let { list ->
                if (pageSize != null) list.take(pageSize) else list
            }

        return Pageable<Thread>().also {
            it.entities = threads
            it.paging = SlackMapper.threadPaging(paging)
        }
    }

    private suspend fun getThreadMemberIds(
        conversation: Conversation,
        userMeId: String,
    ): List<String> {
        val listedMemberIds = SlackMapper.threadMemberIds(conversation, userMeId)
        val channelId = conversation.id
        if (
            listedMemberIds.isNotEmpty() ||
            !SlackMapper.isGroupThread(conversation) ||
            channelId == null
        ) {
            return listedMemberIds
        }

        val response = proceed {
            auth.accessor.slack.conversations().conversationsMembers(
                ConversationsMembersRequest(
                    token = auth.accessor.token,
                    channel = channelId,
                    cursor = null,
                    limit = 1000
                )
            )
        }
        if (!response.isOk) {
            return emptyList()
        }
        return response.members
            ?.filter { it != userMeId }
            ?.distinct()
            ?: emptyList()
    }

    suspend fun sendMessage(req: CommentForm) {
        val token = auth.accessor.token
        @Suppress("UNCHECKED_CAST")
        var channel = req.params["channel"] as? String

        if (channel == null && req.replyId != null) {
            channel = searchMessageChannel(req.replyId!!.value<String>())
        }

        proceedUnit {
            val response = auth.accessor.slack.chat().chatPostMessage(
                work.socialhub.kslack.api.methods.request.chat.ChatPostMessageRequest(
                    token = token,
                    username = null,
                    threadTs = req.replyId?.value<String>(),
                    channel = channel,
                    text = req.text,
                    parse = null,
                    isLinkNames = false,
                    blocks = null,
                    blocksAsString = null,
                    attachments = null,
                    attachmentsAsString = null,
                    isUnfurlLinks = false,
                    isUnfurlMedia = false,
                    isAsUser = null,
                    iconUrl = null,
                    iconEmoji = null,
                    isReplyBroadcast = false
                )
            )

            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Failed to send message")
            }
        }
    }

    suspend fun searchMessageChannel(userId: String): String {
        return proceed {
            val response = auth.accessor.slack.conversations().conversationsList(
                ConversationsListRequest(
                    token = auth.accessor.token,
                    cursor = null,
                    isExcludeArchived = false,
                    limit = 100,
                    types = arrayOf(ConversationType.IM)
                )
            )

            response.channels?.find { it.user == userId }?.id?.let {
                return@proceed it
            }

            val openResponse = auth.accessor.slack.conversations().conversationsOpen(
                ConversationsOpenRequest(
                    token = auth.accessor.token,
                    channel = null,
                    isReturnIm = false,
                    users = arrayOf(userId)
                )
            )

            openResponse.channel?.id!!
        }
    }

    suspend fun getCommentContexts(id: Identify): Context {
        val emojis = getEmojis()
        val userMe = action.userMeWithCache()
        val teamObj = loadTeam()
        val threadId = (id as? SlackComment)?.threadTs ?: id.id!!.value<String>()
        val channelId = getChannelId(id)

        return proceed {
            val response = auth.accessor.slack.conversations().conversationsReplies(
                ConversationsRepliesRequest(
                    token = auth.accessor.token,
                    isInclusive = false,
                    ts = threadId,
                    cursor = null,
                    limit = 100,
                    channel = channelId,
                    oldest = null,
                    latest = null
                )
            )

            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }

            val messages = response.messages?.toList() ?: emptyList()
            val userIds = messages.mapNotNull { m -> m.user }.distinct()
            val userMap = userIds.associateWith { uid ->
                val key = uid
                userCache[key] ?: run {
                    val resp = auth.accessor.slack.users().usersInfo(
                        UsersInfoRequest(token = auth.accessor.token, user = key, isIncludeLocale = false)
                    )
                    val u = SlackMapper.user(resp, teamObj, service)
                    u?.let { userCache[it.id!!.value<String>()] = it }
                    u!!
                }
            }

            val context = Context()
            context.ancestors = mutableListOf()
            context.descendants = mutableListOf()

            var isProceededMine = false
            for (m in messages) {
                if (m.ts == id.id!!.value<String>()) {
                    isProceededMine = true
                    continue
                }

                val user = m.user?.let { userMap[it] }
                val comment = SlackMapper.comment(m, user, userMe, emojis, channelId, service, auth.accessor.token)
                val targetList = if (!isProceededMine) context.ancestors else context.descendants
                (targetList as MutableList).add(comment)
            }

            val allComments = (context.ancestors ?: emptyList()) + (context.descendants ?: emptyList())
            SlackMapper.setMentionName(allComments, userMap)

            context.sort()
            context
        }
    }

    suspend fun getChannelUsers(id: Identify, paging: Paging): Pageable<User> {
        val channelId = id.id!!.value<String>()
        val limitValue = minOf(paging.count ?: 100, 100)

        val response = proceed {
            auth.accessor.slack.conversations().conversationsMembers(
                ConversationsMembersRequest(
                    token = auth.accessor.token,
                    channel = channelId,
                    cursor = null,
                    limit = limitValue
                )
            )
        }

        if (!response.isOk) {
            throw SocialHubException(response.error ?: "Unknown error")
        }

        val allUsers = mutableListOf<User>()
        response.members?.forEach { userId ->
            val user = getUserWithCache(Identify(service, ID(userId)))
            allUsers.add(user)
        }

        return Pageable<User>().also {
            it.entities = allUsers
            it.paging = SlackPaging.fromPaging(paging)
        }
    }

    fun getChannelId(id: Identify): String {
        if (id is SlackComment) return id.channelId!!
        if (id is SlackIdentify) return id.channel!!
        throw SocialHubException("No Channel Info. Identify must be SlackIdentify or SlackComment.")
    }

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Slack,
            statusExtractor = { e -> (e as? SlackApiException)?.response?.status },
            bodyExtractor = { e -> (e as? SlackApiException)?.response?.stringBody },
            runner = runner,
        )
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Slack,
            statusExtractor = { e -> (e as? SlackApiException)?.response?.status },
            bodyExtractor = { e -> (e as? SlackApiException)?.response?.stringBody },
            runner = runner,
        )
    }
}
