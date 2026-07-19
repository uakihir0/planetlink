package work.socialhub.planetlink.discord.action

import work.socialhub.kdiscord.DiscordException
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.define.action.StreamActionType
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Space
import work.socialhub.planetlink.model.Stream
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.utils.ExceptionHandler
import kotlin.js.JsExport

/** Discord プラットフォームのアクション実装 */
@JsExport
class DiscordAction(
    account: Account,
    val auth: DiscordAuth,
) : AccountActionImpl(account) {

    internal val helper = DiscordActionHelper(this)

    // ---------------------------------------------------------------- //
    // Account
    // ---------------------------------------------------------------- //

    override suspend fun userMe(): User {
        return helper.fetchUserMe()
    }

    /**
     * Overrides the base `userMeWithCache()`; delegates to the helper's
     * free-standing `fetchUserMe()` to avoid the Kotlin/JS yield* bridge crash.
     */
    override suspend fun userMeWithCache(): User {
        return me ?: helper.fetchUserMe()
    }

    override suspend fun user(id: Identify): User {
        return helper.getUser(id)
    }

    // ---------------------------------------------------------------- //
    // Timeline (channel messages)
    // ---------------------------------------------------------------- //

    override suspend fun channelTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return helper.getChannelTimeLine(id.id!!.value(), paging)
    }

    override suspend fun messageTimeLine(id: Identify, paging: Paging): Pageable<Comment> {
        return helper.getChannelTimeLine(id.id!!.value(), paging)
    }

    // ---------------------------------------------------------------- //
    // Comment (message)
    // ---------------------------------------------------------------- //

    override suspend fun comment(id: Identify): Comment {
        return helper.getComment(id)
    }

    override suspend fun postComment(req: CommentForm) {
        helper.postComment(req)
    }

    override suspend fun postMessage(req: CommentForm) {
        helper.postComment(req)
    }

    override suspend fun deleteComment(id: Identify) {
        helper.deleteComment(id)
    }

    override suspend fun likeComment(id: Identify) {
        helper.likeComment(id)
    }

    override suspend fun unlikeComment(id: Identify) {
        helper.unlikeComment(id)
    }

    override suspend fun reactionComment(id: Identify, reaction: String) {
        helper.reactionComment(id, reaction)
    }

    override suspend fun unreactionComment(id: Identify, reaction: String) {
        helper.unreactionComment(id, reaction)
    }

    // ---------------------------------------------------------------- //
    // Spaces (Guilds) / Channels / Threads
    // ---------------------------------------------------------------- //

    override suspend fun spaces(paging: Paging): Pageable<Space> {
        return helper.fetchSpaces(paging)
    }

    override suspend fun channels(id: Identify, paging: Paging): Pageable<Channel> {
        return helper.channels(id.id!!.value(), paging)
    }

    override suspend fun messageThread(paging: Paging): Pageable<Thread> {
        return helper.messageThread(paging)
    }

    // ---------------------------------------------------------------- //
    // Stream
    // ---------------------------------------------------------------- //

    override suspend fun setHomeTimeLineStream(callback: EventCallback): Stream {
        return helper.homeTimeLineStream(callback)
    }

    override suspend fun setNotificationStream(callback: EventCallback): Stream {
        throw NotSupportedException("Discord has no REST/Gateway notification stream.")
    }

    // ---------------------------------------------------------------- //
    // Meta
    // ---------------------------------------------------------------- //

    override fun capabilities(): Capabilities = CAPABILITIES

    override fun request(): RequestAction {
        return DiscordRequest(account)
    }

    // ---------------------------------------------------------------- //
    // Internal helpers (used by DiscordActionHelper)
    // ---------------------------------------------------------------- //

    internal fun cacheMe(user: User) {
        me = user
    }

    internal suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Discord,
            statusExtractor = { e -> (e as? DiscordException)?.status },
            bodyExtractor = { e -> (e as? DiscordException)?.body },
            runner = runner,
        )
    }

    internal suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Discord,
            statusExtractor = { e -> (e as? DiscordException)?.status },
            bodyExtractor = { e -> (e as? DiscordException)?.body },
            runner = runner,
        )
    }

    companion object {
        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.GetComment,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.ReactionComment,
                SocialActionType.UnreactionComment,
                SocialActionType.GetSpaces,
                SocialActionType.GetChannels,

                TimeLineActionType.ChannelTimeLine,
                TimeLineActionType.MessageTimeLine,

                MessageActionType.GetMessageThread,
                MessageActionType.GetMessageTimeLine,
                MessageActionType.PostMessage,

                StreamActionType.HomeTimeLineStream,
            )
        )
    }
}
