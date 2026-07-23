package work.socialhub.planetlink.x.action

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import work.socialhub.kxweb.XWeb
import work.socialhub.kxweb.api.AccountResource
import work.socialhub.kxweb.api.BookmarkResource
import work.socialhub.kxweb.api.EngagementResource
import work.socialhub.kxweb.api.ExploreResource
import work.socialhub.kxweb.api.FollowResource
import work.socialhub.kxweb.api.HomeResource
import work.socialhub.kxweb.api.ListResource
import work.socialhub.kxweb.api.MediaResource
import work.socialhub.kxweb.api.PostResource
import work.socialhub.kxweb.api.SearchResource
import work.socialhub.kxweb.api.TimelineResource
import work.socialhub.kxweb.api.TrendResource
import work.socialhub.kxweb.api.TweetResource
import work.socialhub.kxweb.api.UserResource
import work.socialhub.kxweb.entity.account.GetCurrentUserResponse
import work.socialhub.kxweb.entity.share.Response
import work.socialhub.kxweb.entity.user.AboutAccountResponse
import work.socialhub.kxweb.entity.user.FollowingRequest
import work.socialhub.kxweb.entity.user.FollowingResponse
import work.socialhub.kxweb.entity.user.GetUserAboutAccountRequest
import work.socialhub.kxweb.entity.user.GetUserIdByUsernameRequest
import work.socialhub.kxweb.entity.user.UserByScreenNameRequest
import work.socialhub.kxweb.entity.user.UserTweetsRequest
import work.socialhub.kxweb.entity.user.UserTweetsResponse
import work.socialhub.kxweb.model.User as KUser
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.x.model.XUser

internal class XActionTest {

    @Test
    fun usesEmbeddedCurrentUserProfile() = runTest {
        val accountResource = FakeAccountResource(
            GetCurrentUserResponse(
                userId = "123",
                screenName = "planetlink",
                name = "PlanetLink",
                user = KUser(
                    id = "123",
                    screenName = "planetlink",
                    name = "PlanetLink",
                    description = "Kotlin Multiplatform",
                    profileImageUrl = "https://pbs.twimg.com/profile_normal.jpg",
                    followersCount = 10,
                    followingCount = 20,
                    verified = true,
                    location = "Tokyo",
                ),
            )
        )
        val client = FakeXWeb(accountResource)
        val account = Account().also {
            it.service = Service("twitter", it)
        }
        val action = XAction(account, XAuth(), client, guest = false).also {
            account.action = it
        }

        val user = assertIs<XUser>(action.userMe())

        assertEquals("123", user.id<String>())
        assertEquals("Kotlin Multiplatform", user.description?.displayText)
        assertEquals("https://pbs.twimg.com/profile_400x400.jpg", user.iconImageUrl)
        assertEquals(10, user.followersCount)
        assertEquals(20, user.followingCount)
        assertEquals(true, user.verified)
        assertEquals("Tokyo", user.location)
        assertSame(user, action.userMeWithCache())
        assertEquals(1, accountResource.calls)
        assertEquals(0, client.userResourceCalls)
    }

    @Test
    fun fallsBackToScreenNameWhenCurrentUserProfileIsMissing() = runTest {
        val accountResource = FakeAccountResource(
            GetCurrentUserResponse(
                userId = "123",
                screenName = "planetlink",
                name = "PlanetLink",
            )
        )
        val userResource = FakeUserResource(
            KUser(
                id = "123",
                screenName = "planetlink",
                name = "PlanetLink",
            )
        )
        val client = FakeXWeb(accountResource, userResource)
        val account = Account().also {
            it.service = Service("twitter", it)
        }
        val action = XAction(account, XAuth(), client, guest = false).also {
            account.action = it
        }

        val user = assertIs<XUser>(action.userMe())

        assertEquals("123", user.id<String>())
        assertEquals("planetlink", user.screenName)
        assertEquals(1, accountResource.calls)
        assertEquals(1, client.userResourceCalls)
        assertEquals(1, userResource.calls)
    }

    private class FakeAccountResource(
        private val currentUser: GetCurrentUserResponse,
    ) : AccountResource {

        var calls = 0

        override suspend fun getCurrentUser(): Response<GetCurrentUserResponse> {
            calls++
            return Response(currentUser, "{}")
        }

        override fun getCurrentUserBlocking(): Response<GetCurrentUserResponse> {
            error("Blocking access is not expected.")
        }
    }

    private class FakeXWeb(
        private val accountResource: AccountResource,
        private val userResource: UserResource? = null,
    ) : XWeb {

        var userResourceCalls = 0

        override fun account(): AccountResource = accountResource

        override fun user(): UserResource {
            userResourceCalls++
            return userResource
                ?: error("The embedded current-user profile should avoid a second user lookup.")
        }

        override fun search(): SearchResource = unsupported()
        override fun tweet(): TweetResource = unsupported()
        override fun home(): HomeResource = unsupported()
        override fun engagement(): EngagementResource = unsupported()
        override fun post(): PostResource = unsupported()
        override fun follow(): FollowResource = unsupported()
        override fun bookmark(): BookmarkResource = unsupported()
        override fun list(): ListResource = unsupported()
        override fun media(): MediaResource = unsupported()
        override fun explore(): ExploreResource = unsupported()
        override fun timeline(): TimelineResource = unsupported()
        override fun trend(): TrendResource = unsupported()

        private fun <T> unsupported(): T {
            error("Resource is not expected in this test.")
        }
    }

    private class FakeUserResource(
        private val user: KUser,
    ) : UserResource {

        var calls = 0

        override suspend fun getUserByScreenName(
            request: UserByScreenNameRequest,
        ): Response<KUser> {
            calls++
            return Response(user, "{}")
        }

        override fun getUserByScreenNameBlocking(
            request: UserByScreenNameRequest,
        ): Response<KUser> = unsupported()

        override suspend fun getUserIdByUsername(
            request: GetUserIdByUsernameRequest,
        ): Response<KUser> = unsupported()

        override fun getUserIdByUsernameBlocking(
            request: GetUserIdByUsernameRequest,
        ): Response<KUser> = unsupported()

        override suspend fun getUserAboutAccount(
            request: GetUserAboutAccountRequest,
        ): Response<AboutAccountResponse> = unsupported()

        override fun getUserAboutAccountBlocking(
            request: GetUserAboutAccountRequest,
        ): Response<AboutAccountResponse> = unsupported()

        override suspend fun getUserTweets(
            request: UserTweetsRequest,
        ): Response<UserTweetsResponse> = unsupported()

        override fun getUserTweetsBlocking(
            request: UserTweetsRequest,
        ): Response<UserTweetsResponse> = unsupported()

        override suspend fun getFollowing(
            request: FollowingRequest,
        ): Response<FollowingResponse> = unsupported()

        override fun getFollowingBlocking(
            request: FollowingRequest,
        ): Response<FollowingResponse> = unsupported()

        override suspend fun getFollowers(
            request: FollowingRequest,
        ): Response<FollowingResponse> = unsupported()

        override fun getFollowersBlocking(
            request: FollowingRequest,
        ): Response<FollowingResponse> = unsupported()

        private fun unsupported(): Nothing {
            error("User resource operation is not expected in this test.")
        }
    }
}
