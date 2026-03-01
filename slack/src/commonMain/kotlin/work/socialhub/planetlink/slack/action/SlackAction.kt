package work.socialhub.planetlink.slack.action

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import work.socialhub.kslack.api.methods.request.auth.AuthTestRequest
import work.socialhub.kslack.api.methods.request.bots.BotsInfoRequest
import work.socialhub.kslack.api.methods.request.chat.ChatDeleteRequest
import work.socialhub.kslack.api.methods.request.chat.ChatPostMessageRequest
import work.socialhub.kslack.api.methods.request.conversations.*
import work.socialhub.kslack.api.methods.request.emoji.EmojiListRequest
import work.socialhub.kslack.api.methods.request.reactions.ReactionsAddRequest
import work.socialhub.kslack.api.methods.request.reactions.ReactionsRemoveRequest
import work.socialhub.kslack.api.methods.request.team.TeamInfoRequest
import work.socialhub.kslack.api.methods.request.users.UsersInfoRequest
import work.socialhub.kslack.api.methods.request.users.UsersListRequest
import work.socialhub.kslack.entity.Attachment
import work.socialhub.kslack.entity.ConversationType
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.paging.DatePaging
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.slack.model.*

class SlackAction(
    account: Account,
    val auth: SlackAuth
) : AccountActionImpl(account) {

    private var team: SlackTeam? = null
    private var generalChannel: String? = null

    private val userCache = mutableMapOf<String, User>()
    private val botCache = mutableMapOf<String, User>()
    private var emojisCache: List<Emoji>? = null

    private var userMeId: String? = null

    override suspend fun userMe(): User {
        return proceed<User> {
            val testResponse = auth.accessor.slack.auth().authTest(
                AuthTestRequest(auth.accessor.token)
            )

            if (!testResponse.isOk) {
                throw SocialHubException(testResponse.error ?: "Unknown error")
            }

            userMeId = testResponse.userId

            val userId = testResponse.userId ?: throw SocialHubException("User ID is null")
            val userResponse = auth.accessor.slack.users().usersInfo(
                UsersInfoRequest(
                    token = auth.accessor.token,
                    user = userId,
                    isIncludeLocale = false
                )
            )

            val teamObj = loadTeam()
            val user = SlackMapper.user(userResponse, teamObj, service())
            user?.let { userCache[it.id!!.value<String>()] = it }
            me = user
            user!!
        }
    }

    override suspend fun user(id: Identify): User {
        val key = id.id!!.value<String>()
        userCache[key]?.let { return it }

        return proceed<User> {
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

            val teamObj = loadTeam()
            val user = SlackMapper.user(response, teamObj, service())
            user?.let { userCache[it.id!!.value<String>()] = it }
            user!!
        }
    }

    override suspend fun user(url: String): User {
        throw NotSupportedException("Slack does not support user URL lookup")
    }

    override suspend fun followingUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException("Slack does not have follow functionality")
    }

    override suspend fun followerUsers(id: Identify, paging: Paging): Pageable<User> {
        throw NotSupportedException("Slack does not have follower functionality")
    }

    override suspend fun searchUsers(query: String, paging: Paging): Pageable<User> {
        return proceed<Pageable<User>> {
            if (userCache.isEmpty()) {
                loadUsersCache()
            }

            val filtered = userCache.values.filter {
                (it as? SlackUser)?.screenName?.contains(query, ignoreCase = true) == true ||
                it.name.contains(query, ignoreCase = true)
            }

            Pageable<User>().also {
                it.entities = filtered
                it.paging = paging
            }
        }
    }

    private suspend fun loadUsersCache() {
        val response = auth.accessor.slack.users().usersList(
            UsersListRequest(
                token = auth.accessor.token,
                cursor = null,
                limit = 200,
                isIncludeLocale = false,
                isPresence = false
            )
        )

        if (!response.isOk) {
            throw SocialHubException(response.error ?: "Unknown error")
        }

        val teamObj = loadTeam()
        response.members?.forEach { u ->
            val user = SlackMapper.user(u, teamObj, service())
            user?.let { userCache[it.id!!.value<String>()] = it }
        }
    }

    override suspend fun homeTimeLine(paging: Paging): Pageable<Comment> {
        return channelTimeLine(Identify(service(), ID(loadGeneralChannel())), paging)
    }

    override suspend fun mentionTimeLine(paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Slack does not support mention timeline")
    }

    override suspend fun userCommentTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Slack does not support user timeline")
    }

    override suspend fun userLikeTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Slack does not support like timeline")
    }

    override suspend fun userMediaTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Slack does not support media timeline")
    }

    override suspend fun searchTimeLine(query: String, paging: Paging): Pageable<Comment> {
        throw NotSupportedException("Use search API directly for Slack search")
    }

    override suspend fun postComment(req: CommentForm) {
        proceedUnit {
            sendMessage(req)
        }
    }

    override suspend fun comment(id: Identify): Comment {
        throw NotSupportedException("Slack messages require channel context")
    }

    override suspend fun comment(url: String): Comment {
        throw NotSupportedException("Slack messages require channel context")
    }

    override suspend fun likeComment(id: Identify) {
        reactionComment(id, "heart")
    }

    override suspend fun unlikeComment(id: Identify) {
        unreactionComment(id, "heart")
    }

    override suspend fun shareComment(id: Identify) {
        throw NotSupportedException("Slack does not support sharing messages")
    }

    override suspend fun unshareComment(id: Identify) {
        throw NotSupportedException("Slack does not support sharing messages")
    }

    override suspend fun reactionComment(id: Identify, reaction: String) {
        proceedUnit {
            val channelId = getChannelId(id)
            val timestamp = id.id!!.value<String>()
            auth.accessor.slack.reactions().reactionsAdd(
                ReactionsAddRequest(
                    token = auth.accessor.token,
                    name = reaction,
                    file = null,
                    fileComment = null,
                    channel = channelId,
                    timestamp = timestamp
                )
            )
        }
    }

    override suspend fun unreactionComment(id: Identify, reaction: String) {
        proceedUnit {
            val channelId = getChannelId(id)
            val timestamp = id.id!!.value<String>()
            auth.accessor.slack.reactions().reactionsRemove(
                ReactionsRemoveRequest(
                    token = auth.accessor.token,
                    name = reaction,
                    file = null,
                    fileComment = null,
                    channel = channelId,
                    timestamp = timestamp
                )
            )
        }
    }

    override suspend fun deleteComment(id: Identify) {
        proceedUnit {
            val channelId = getChannelId(id)
            val ts = id.id!!.value<String>()
            auth.accessor.slack.chat().chatDelete(
                ChatDeleteRequest(
                    token = auth.accessor.token,
                    ts = ts,
                    channel = channelId,
                    isAsUser = false
                )
            )
        }
    }

    override suspend fun commentContexts(id: Identify): Context {
        return proceed<Context> {
            coroutineScope {
                val threadId = (id as? SlackComment)?.threadTs ?: id.id!!.value<String>()
                val channelId = getChannelId(id)

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

                val emojis = getEmojis()
                val userMe = userMeWithCache()

                val messages = response.messages?.toList() ?: emptyList()
                val userIds = messages.mapNotNull { m -> m.user }.distinct()
                val userMap = userIds.associateWith { uid -> getUserWithCache(Identify(service(), ID(uid))) }

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
                    val comment = SlackMapper.comment(m, user, userMe, emojis, channelId, service(), auth.accessor.token)
                    val targetList = if (!isProceededMine) context.ancestors else context.descendants
                    (targetList as MutableList).add(comment)
                }

                val allComments = (context.ancestors ?: emptyList()) + (context.descendants ?: emptyList())
                SlackMapper.setMentionName(allComments, userMap)

                context.sort()
                context
            }
        }
    }

    override fun emojis(): List<Emoji> {
        return emojisCache ?: super.emojis()
    }

    suspend fun getEmojis(): List<Emoji> {
        if (emojisCache != null) return emojisCache!!

        return proceed<List<Emoji>> {
            val response = auth.accessor.slack.emoji().emojiList(
                EmojiListRequest(auth.accessor.token)
            )

            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }

            val emojis = SlackMapper.emojis(response)
            emojisCache = emojis + super.emojis()
            emojisCache!!
        }
    }

    override suspend fun channels(id: Identify, paging: Paging): Pageable<Channel> {
        return proceed<Pageable<Channel>> {
            val response = auth.accessor.slack.conversations().conversationsList(
                ConversationsListRequest(
                    token = auth.accessor.token,
                    cursor = null,
                    isExcludeArchived = false,
                    limit = 1000,
                    types = arrayOf(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL)
                )
            )

            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }

            response.channels?.find { it.isGeneral }?.let {
                generalChannel = it.id
            }

            SlackMapper.channels(response, service())
        }
    }

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return getChannelTimeLine(id.id!!.value<String>(), paging)
    }

    override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
        return proceed<Pageable<User>> {
            val channelId = id.id!!.value<String>()
            val allUsers = mutableListOf<User>()
            val limitValue = minOf(paging.count ?: 100, 100)

            val response = auth.accessor.slack.conversations().conversationsMembers(
                ConversationsMembersRequest(
                    token = auth.accessor.token,
                    channel = channelId,
                    cursor = null,
                    limit = limitValue
                )
            )

            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }

            response.members?.forEach { userId ->
                val user = getUserWithCache(Identify(service(), ID(userId)))
                allUsers.add(user)
            }

            Pageable<User>().also {
                it.entities = allUsers
                it.paging = SlackPaging.fromPaging(paging)
            }
        }
    }

    override suspend fun messageThread(paging: Paging): Pageable<Thread> {
        return proceed<Pageable<Thread>> {
            coroutineScope {
                val response = auth.accessor.slack.conversations().conversationsList(
                    ConversationsListRequest(
                        token = auth.accessor.token,
                        cursor = null,
                        isExcludeArchived = false,
                        limit = paging.count ?: 200,
                        types = arrayOf(ConversationType.IM, ConversationType.MPIM)
                    )
                )

                if (!response.isOk) {
                    throw SocialHubException(response.error ?: "Unknown error")
                }

                val userMe = userMeWithCache()
                val memberMap = mutableMapOf<String, List<String>>()
                val historyMap = mutableMapOf<String, Instant>()

                response.channels?.forEach { channel ->
                    val channelUser = channel.user
                    val channelId = channel.id
                    if (channelUser != null && channelId != null) {
                        memberMap[channelId] = listOf(userMe.id!!.value<String>(), channelUser)
                    }
                }

                val allUserIds = memberMap.values.flatten().distinct()
                val accountMap = allUserIds.associateWith { uid ->
                    getUserWithCache(Identify(service(), ID(uid)))
                }

                val threads = SlackMapper.threads(response, memberMap, historyMap, accountMap, service())
                    .sortedByDescending { it.lastUpdate }

                Pageable<Thread>().also {
                    it.entities = threads
                    it.paging = paging
                }
            }
        }
    }

    override suspend fun messageTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return getChannelTimeLine(id.id!!.value<String>(), paging)
    }

    override suspend fun postMessage(req: CommentForm) {
        proceedUnit {
            sendMessage(req)
        }
    }

    override suspend fun setHomeTimeLineStream(callback: EventCallback): Stream {
        throw NotSupportedException("Use Socket Mode or RTM for Slack streaming")
    }

    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        throw NotSupportedException("Use Socket Mode or RTM for Slack streaming")
    }

    override fun request(): RequestAction {
        return SlackRequest(account)
    }

    fun getTeam(): SlackTeam? = team

    private suspend fun loadTeam(): SlackTeam? {
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

    suspend fun getBots(id: Identify): User {
        val key = id.id!!.value<String>()
        botCache[key]?.let { return it }

        return proceed<User> {
            val response = auth.accessor.slack.bots().botsInfo(
                BotsInfoRequest(
                    token = auth.accessor.token,
                    bot = key
                )
            )

            if (!response.isOk) {
                throw SocialHubException(response.error ?: "Unknown error")
            }

            val bot = SlackMapper.bots(response, service())!!
            botCache[bot.id!!.value<String>()] = bot
            bot
        }
    }

    fun getGeneralChannel(): String {
        return generalChannel ?: ""
    }

    private suspend fun loadGeneralChannel(): String {
        if (generalChannel == null) {
            channels(Identify(service()), Paging())
        }
        return generalChannel ?: throw SocialHubException("General channel not found.")
    }

    private suspend fun getChannelTimeLine(channel: String, paging: Paging): Pageable<Comment> {
        return proceed<Pageable<Comment>> {
            coroutineScope {
                val datePaging = if (paging is DatePaging) paging else null

                val responseAsync = async {
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

                val emojisAsync = async { getEmojis() }
                val userMeAsync = async { userMeWithCache() }

                val response = responseAsync.await()
                val emojis = emojisAsync.await()
                val userMe = userMeAsync.await()

                if (!response.isOk) {
                    throw SocialHubException(response.error ?: "Unknown error")
                }

                val messages = response.messages?.toList() ?: emptyList()

                val userIds = messages
                    .filter { it.subtype != "bot_message" }
                    .mapNotNull { it.user }
                    .distinct()
                val userMap = userIds.associateWith { getUserWithCache(Identify(service(), ID(it))) }

                val botIds = messages
                    .filter { it.subtype == "bot_message" }
                    .mapNotNull { it.botId }
                    .distinct()
                val botMap = botIds.associateWith { getBotWithCache(Identify(service(), ID(it))) }

                val pageable = SlackMapper.timeLine(messages, userMap, botMap, userMe, emojis, channel, service(), paging, auth.accessor.token)

                val allComments = pageable.entities + pageable.displayableEntities
                SlackMapper.setMentionName(allComments, userMap)

                pageable
            }
        }
    }

    private suspend fun sendMessage(req: CommentForm) {
        val token = auth.accessor.token
        @Suppress("UNCHECKED_CAST")
        var channel = req.params["channel"] as? String

        if (channel == null && req.replyId != null) {
            channel = searchMessageChannel(req.replyId!!.value<String>())
        }

        val response = auth.accessor.slack.chat().chatPostMessage(
            ChatPostMessageRequest(
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

    private suspend fun searchMessageChannel(userId: String): String {
        return proceed<String> {
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

    private fun getChannelId(id: Identify): String {
        if (id is SlackComment) return id.channelId!!
        if (id is SlackIdentify) return id.channel!!

        throw SocialHubException("No Channel Info. Identify must be SlackIdentify or SlackComment.")
    }

    private suspend fun getUserWithCache(id: Identify): User {
        val key = id.id!!.value<String>()
        val cached = userCache[key]
        if (cached != null) return cached
        return user(id)
    }

    private suspend fun getBotWithCache(id: Identify): User {
        val key = id.id!!.value<String>()
        val cached = botCache[key]
        if (cached != null) return cached
        return getBots(id)
    }

    private fun service(): Service = account.service

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return try {
            runner()
        } catch (e: SocialHubException) {
            throw e
        } catch (e: Exception) {
            throw SocialHubException(e.message, e)
        }
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        try {
            runner()
        } catch (e: SocialHubException) {
            throw e
        } catch (e: Exception) {
            throw SocialHubException(e.message, e)
        }
    }
}
