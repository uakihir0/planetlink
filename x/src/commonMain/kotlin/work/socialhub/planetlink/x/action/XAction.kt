package work.socialhub.planetlink.x.action

import io.ktor.http.Url
import kotlin.js.JsExport
import work.socialhub.kxweb.XWeb
import work.socialhub.kxweb.XWebException
import work.socialhub.kxweb.entity.bookmark.GetBookmarksRequest
import work.socialhub.kxweb.entity.home.HomeTimelineRequest
import work.socialhub.kxweb.entity.search.SearchSearchRequest
import work.socialhub.kxweb.entity.search.SearchType
import work.socialhub.kxweb.entity.search.SearchUsersRequest
import work.socialhub.kxweb.entity.timeline.GetLikesRequest
import work.socialhub.kxweb.entity.trend.GetTrendsRequest
import work.socialhub.kxweb.entity.tweet.TweetDetailRequest
import work.socialhub.kxweb.entity.user.FollowingRequest
import work.socialhub.kxweb.entity.user.UserByScreenNameRequest
import work.socialhub.kxweb.entity.user.UserTweetsRequest
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Trend
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.utils.ExceptionHandler
import work.socialhub.planetlink.x.define.XActionType
import work.socialhub.planetlink.x.model.XComment
import work.socialhub.planetlink.x.model.XPaging
import work.socialhub.planetlink.x.model.XTrendLocation
import work.socialhub.planetlink.x.model.XUser

@JsExport
class XAction(
    account: Account,
    val auth: XAuth,
    private val client: XWeb,
    private val guest: Boolean,
) : AccountActionImpl(account) {

    override fun capabilities(): Capabilities {
        return if (guest) GUEST_CAPABILITIES else AUTHENTICATED_CAPABILITIES
    }

    override suspend fun userMe(): User {
        return fetchUserMe()
    }

    private suspend fun fetchUserMe(): User {
        return proceed {
            val current = client.account().getCurrentUser().data
            val screenName = checkNotNull(current.screenName) {
                "The authenticated X account has no screen name."
            }
            fetchUserByScreenName(screenName).also { me = it }
        }
    }

    override suspend fun userMeWithCache(): User {
        return me ?: fetchUserMe()
    }

    override suspend fun user(id: Identify): User {
        return fetchUser(id)
    }

    private suspend fun fetchUser(id: Identify): User {
        val screenName = when (id) {
            is XUser -> id.screenName
            else -> id.id<String>().takeUnless { value -> value.all(Char::isDigit) }
        } ?: throw NotSupportedException(
            "kxweb requires an X screen name when resolving a user."
        )
        return fetchUserByScreenName(screenName)
    }

    private suspend fun fetchUserByScreenName(
        screenName: String,
    ): XUser {
        return proceed {
            val source = client.user().getUserByScreenName(
                UserByScreenNameRequest().also {
                    it.screenName = screenName.removePrefix("@")
                }
            ).data
            XMapper.user(source, service())
        }
    }

    override suspend fun user(url: String): User {
        val parsed = parseXUrl(url)
        val screenName = parsed.segments.firstOrNull()
            ?: throw NotSupportedException("The X user URL has no screen name.")
        return fetchUserByScreenName(screenName)
    }

    override suspend fun followingUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return fetchFollowingUsers(id, paging, followers = false)
    }

    override suspend fun followerUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return fetchFollowingUsers(id, paging, followers = true)
    }

    private suspend fun fetchFollowingUsers(
        id: Identify,
        paging: Paging,
        followers: Boolean,
    ): Pageable<User> {
        return proceed {
            val request = FollowingRequest().also {
                it.userId = resolveUserId(id)
                it.count = limit(paging)
                it.cursor = cursor(paging)
            }
            val response = if (followers) {
                client.user().getFollowers(request).data
            } else {
                client.user().getFollowing(request).data
            }
            XMapper.users(response.users, service(), paging, response.cursor)
        }
    }

    override suspend fun searchUsers(
        query: String,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val response = client.search().searchUsers(
                SearchUsersRequest().also {
                    it.query = query
                    it.count = limit(paging)
                    it.cursor = cursor(paging)
                }
            ).data
            XMapper.users(response.users, service(), paging, response.cursor)
        }
    }

    override suspend fun homeTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        return fetchHomeTimeLine(paging, recommended = false)
    }

    suspend fun recommendedTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        return fetchHomeTimeLine(paging, recommended = true)
    }

    private suspend fun fetchHomeTimeLine(
        paging: Paging,
        recommended: Boolean,
    ): Pageable<Comment> {
        return proceed {
            val request = HomeTimelineRequest().also {
                it.count = limit(paging)
                it.cursor = cursor(paging)
            }
            val response = if (recommended) {
                client.home().getHomeTimeline(request).data
            } else {
                client.home().getHomeLatestTimeline(request).data
            }
            XMapper.timeline(response.tweets, service(), paging, response.cursor)
        }
    }

    override suspend fun mentionTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        val screenName = ((me ?: fetchUserMe()) as XUser).screenName.orEmpty()
        return fetchSearchTimeLine("@$screenName -from:$screenName", paging)
    }

    override suspend fun userCommentTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return fetchUserCommentTimeLine(id, paging)
    }

    private suspend fun fetchUserCommentTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response = client.user().getUserTweets(
                UserTweetsRequest().also {
                    it.userId = resolveUserId(id)
                    it.count = limit(paging)
                    it.cursor = cursor(paging)
                }
            ).data
            XMapper.timeline(response.tweets, service(), paging, response.cursor)
        }
    }

    override suspend fun userLikeTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response = client.timeline().getLikes(
                GetLikesRequest().also {
                    it.userId = resolveUserId(id)
                    it.count = limit(paging)
                    it.cursor = cursor(paging)
                }
            ).data
            XMapper.timeline(response.tweets, service(), paging, response.cursor)
        }
    }

    override suspend fun userMediaTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        val screenName = resolveScreenName(id)
        return fetchSearchTimeLine("from:$screenName filter:media", paging)
    }

    override suspend fun searchTimeLine(
        query: String,
        paging: Paging,
    ): Pageable<Comment> {
        return fetchSearchTimeLine(query, paging)
    }

    private suspend fun fetchSearchTimeLine(
        query: String,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response = client.search().searchTweets(
                SearchSearchRequest().also {
                    it.query = query
                    it.count = limit(paging)
                    it.cursor = cursor(paging)
                    it.searchType = SearchType.LATEST
                }
            ).data
            XMapper.timeline(response.tweets, service(), paging, response.cursor)
        }
    }

    override suspend fun userBookmarkTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response = client.bookmark().getBookmarks(
                GetBookmarksRequest().also {
                    it.count = limit(paging)
                    it.cursor = cursor(paging)
                }
            ).data
            XMapper.timeline(response.tweets, service(), paging, response.cursor)
        }
    }

    override suspend fun comment(id: Identify): Comment {
        return fetchComment(id.id())
    }

    override suspend fun comment(url: String): Comment {
        val parsed = parseXUrl(url)
        val segments = parsed.segments
        val statusIndex = segments.indexOf("status")
        if (statusIndex < 0 || statusIndex + 1 >= segments.size) {
            throw NotSupportedException("The URL is not an X post URL.")
        }
        return fetchComment(segments[statusIndex + 1])
    }

    private suspend fun fetchComment(id: String): XComment {
        return proceed {
            val source = client.tweet().getTweet(id, withArticle = true).data
            XMapper.comment(source, service())
        }
    }

    override suspend fun commentContexts(
        id: Identify,
    ): Context {
        return fetchCommentContexts(id.id())
    }

    private suspend fun fetchCommentContexts(
        id: String,
    ): Context {
        return proceed {
            val response = client.tweet().getTweetDetail(
                TweetDetailRequest().also { it.tweetId = id }
            ).data
            val comments = response.tweets.map { XMapper.comment(it, service()) }
            val focal = comments.firstOrNull { it.id<String>() == id }
            val focalTime = focal?.createAt

            Context().also { context ->
                context.ancestors = comments.filter {
                    it.id<String>() != id &&
                        focalTime != null &&
                        it.createAt != null &&
                        it.createAt!! < focalTime
                }.sortedByDescending { it.createAt }
                context.descendants = comments.filter {
                    it.id<String>() != id && it !in context.ancestors.orEmpty()
                }.sortedByDescending { it.createAt }
            }
        }
    }

    suspend fun trends(
        woeid: Long = 1,
    ): List<Trend> {
        return proceed {
            client.trend().getTrends(
                GetTrendsRequest().also { it.woeid = woeid }
            ).data.trends.map(XMapper::trend)
        }
    }

    suspend fun trendLocations(): List<XTrendLocation> {
        return proceed {
            client.trend().getTrendLocations()
                .data.locations.map(XMapper::trendLocation)
        }
    }

    private suspend fun resolveUserId(id: Identify): String {
        if (id is XUser) {
            return id.id()
        }
        val value = id.id<String>()
        if (value.all(Char::isDigit)) {
            return value
        }
        return fetchUserByScreenName(value).id()
    }

    private suspend fun resolveScreenName(id: Identify): String {
        if (id is XUser) {
            return checkNotNull(id.screenName)
        }
        val value = id.id<String>().removePrefix("@")
        if (!value.all(Char::isDigit)) {
            return value
        }
        throw NotSupportedException(
            "kxweb requires an X screen name for this timeline."
        )
    }

    private fun parseXUrl(value: String): Url {
        val url = try {
            Url(value)
        } catch (e: Exception) {
            throw NotSupportedException("Invalid X URL: ${e.message}")
        }
        if (url.host.lowercase() !in X_HOSTS) {
            throw NotSupportedException("The URL does not belong to X.")
        }
        return url
    }

    private fun cursor(paging: Paging?): String? {
        return (paging as? XPaging)?.currentCursor
    }

    private fun limit(paging: Paging?): Int {
        return paging?.count ?: DEFAULT_COUNT
    }

    private fun service(): Service {
        return account.service
    }

    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Twitter,
            statusExtractor = { exception ->
                (exception as? XWebException)?.status
                    ?: (exception.cause as? XWebException)?.status
            },
            bodyExtractor = { exception ->
                (exception as? XWebException)?.body
                    ?: (exception.cause as? XWebException)?.body
            },
            runner = runner,
        )
    }

    companion object {
        private const val DEFAULT_COUNT = 20
        private val X_HOSTS = setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com")

        val AUTHENTICATED_CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.GetComment,
                SocialActionType.GetContext,
                SocialActionType.GetUserBookmarks,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.MentionTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.UserLikeTimeLine,
                TimeLineActionType.UserMediaTimeLine,
                TimeLineActionType.SearchTimeLine,
                TimeLineActionType.UserBookmarkTimeLine,

                UsersActionType.GetFollowingUsers,
                UsersActionType.GetFollowerUsers,
                UsersActionType.SearchUsers,

                XActionType.RecommendedTimeLine,
                XActionType.GetTrends,
                XActionType.GetTrendLocations,
            )
        )

        val GUEST_CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUser,
                SocialActionType.GetComment,
                XActionType.GetTrends,
                XActionType.GetTrendLocations,
            )
        )
    }
}
