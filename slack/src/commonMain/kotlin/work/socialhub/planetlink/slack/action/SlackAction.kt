package work.socialhub.planetlink.slack.action

import work.socialhub.kslack.api.methods.SlackApiException
import work.socialhub.kslack.api.methods.request.auth.AuthTestRequest
import work.socialhub.kslack.api.methods.request.chat.ChatDeleteRequest
import work.socialhub.kslack.api.methods.request.conversations.*
import work.socialhub.kslack.api.methods.request.reactions.ReactionsAddRequest
import work.socialhub.kslack.api.methods.request.reactions.ReactionsRemoveRequest
import work.socialhub.kslack.api.methods.request.users.UsersInfoRequest
import work.socialhub.kslack.entity.ConversationType
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.slack.model.*
import work.socialhub.planetlink.utils.ExceptionHandler
import kotlin.js.JsExport

/** Slack プラットフォームのアクション実装 */
@JsExport
class SlackAction(
    account: Account,
    val auth: SlackAuth
) : AccountActionImpl(account) {

    companion object {
        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.GetContext,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.LikeComment,
                SocialActionType.UnlikeComment,
                SocialActionType.ReactionComment,
                SocialActionType.UnreactionComment,
                SocialActionType.GetChannels,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.ChannelTimeLine,

                UsersActionType.SearchUsers,
                UsersActionType.ChannelUsers,

                MessageActionType.GetMessageThread,
                MessageActionType.GetMessageTimeLine,
                MessageActionType.PostMessage,
            )
        )
    }

    override fun capabilities(): Capabilities = CAPABILITIES

    internal val userCache = mutableMapOf<String, User>()
    internal val botCache = mutableMapOf<String, User>()

    internal val helper = SlackActionHelper(this)

    override suspend fun userMe(): User {
        val teamObj = helper.loadTeam()
        return proceed<User> {
            val testResponse = auth.accessor.slack.auth().authTest(
                AuthTestRequest(auth.accessor.token)
            )

            if (!testResponse.isOk) {
                throw SocialHubException(testResponse.error ?: "Unknown error")
            }

            helper.userMeId = testResponse.userId

            val userId = testResponse.userId ?: throw SocialHubException("User ID is null")
            val userResponse = auth.accessor.slack.users().usersInfo(
                UsersInfoRequest(
                    token = auth.accessor.token,
                    user = userId,
                    isIncludeLocale = false
                )
            )

            val user = SlackMapper.user(userResponse, teamObj, service())
            user?.let { userCache[it.id!!.value<String>()] = it }
            me = user
            user!!
        }
    }

    override suspend fun user(id: Identify): User {
        val key = id.id!!.value<String>()
        userCache[key]?.let { return it }
        return helper.getUserWithCache(id)
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
        if (userCache.isEmpty()) {
            helper.loadUsersCache()
        }
        return proceed<Pageable<User>> {
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

    override suspend fun homeTimeLine(paging: Paging): Pageable<Comment> {
        val channel = helper.loadGeneralChannel()
        return helper.getChannelTimeLine(channel, paging)
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
        helper.sendMessage(req)
    }

    override suspend fun comment(id: Identify): Comment {
        throw NotSupportedException("Slack messages require channel context")
    }

    override suspend fun comment(url: String): Comment {
        throw NotSupportedException("Slack messages require channel context")
    }

    override suspend fun likeComment(id: Identify) {
        proceedUnit {
            val channelId = getChannelId(id)
            val timestamp = id.id!!.value<String>()
            auth.accessor.slack.reactions().reactionsAdd(
                ReactionsAddRequest(
                    token = auth.accessor.token,
                    name = "heart",
                    file = null,
                    fileComment = null,
                    channel = channelId,
                    timestamp = timestamp
                )
            )
        }
    }

    override suspend fun unlikeComment(id: Identify) {
        proceedUnit {
            val channelId = getChannelId(id)
            val timestamp = id.id!!.value<String>()
            auth.accessor.slack.reactions().reactionsRemove(
                ReactionsRemoveRequest(
                    token = auth.accessor.token,
                    name = "heart",
                    file = null,
                    fileComment = null,
                    channel = channelId,
                    timestamp = timestamp
                )
            )
        }
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
        return helper.getCommentContexts(id)
    }

    override fun emojis(): List<Emoji> {
        return helper.emojisCache ?: super.emojis()
    }

    suspend fun getEmojis(): List<Emoji> {
        return helper.getEmojis()
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
                helper.generalChannel = it.id
            }

            SlackMapper.channels(response, service())
        }
    }

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return helper.getChannelTimeLine(id.id!!.value<String>(), paging)
    }

    override suspend fun channelUsers(id: Identify, paging: Paging): Pageable<User> {
        return helper.getChannelUsers(id, paging)
    }

    override suspend fun messageThread(paging: Paging): Pageable<Thread> {
        return helper.getMessageThread(paging)
    }

    override suspend fun messageTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return helper.getChannelTimeLine(id.id!!.value<String>(), paging)
    }

    override suspend fun postMessage(req: CommentForm) {
        helper.sendMessage(req)
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

    suspend fun getBots(id: Identify): User {
        return helper.getBotWithCache(id)
    }

    fun getGeneralChannel(): String {
        return helper.generalChannel ?: ""
    }

    fun getTeam(): SlackTeam? = helper.team

    private fun getChannelId(id: Identify): String {
        return helper.getChannelId(id)
    }

    private fun service(): Service = account.service

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
