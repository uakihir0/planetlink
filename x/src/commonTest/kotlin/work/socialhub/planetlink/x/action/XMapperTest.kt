package work.socialhub.planetlink.x.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import work.socialhub.kxweb.model.Article
import work.socialhub.kxweb.model.Media
import work.socialhub.kxweb.model.Tweet
import work.socialhub.kxweb.model.User
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.x.model.XComment
import work.socialhub.planetlink.x.model.XUser

class XMapperTest {

    private val service = Service("twitter", Account())

    @Test
    fun mapsUser() {
        val user = XMapper.user(
            User(
                id = "123",
                screenName = "planetlink",
                name = "PlanetLink",
                description = "Kotlin Multiplatform",
                profileImageUrl = "https://pbs.twimg.com/profile_normal.jpg",
                profileBannerUrl = "https://pbs.twimg.com/banner.jpg",
                followersCount = 10,
                followingCount = 20,
                statusesCount = 30,
                listedCount = 2,
                verified = true,
                location = "Tokyo",
                url = "https://example.com",
            ),
            service,
        )

        assertEquals("123", user.id<String>())
        assertEquals("@planetlink", user.accountIdentify)
        assertEquals("https://x.com/planetlink", user.webUrl)
        assertEquals("https://pbs.twimg.com/profile_400x400.jpg", user.iconImageUrl)
        assertEquals(10, user.followersCount)
        assertEquals(2, user.listedCount)
        assertEquals(true, user.verified)
    }

    @Test
    fun mapsPostWithMediaAndArticle() {
        val comment = XMapper.comment(
            Tweet(
                id = "456",
                text = "Hello #Kotlin",
                createdAt = "Mon Jan 01 12:34:56 +0000 2024",
                user = User(
                    id = "123",
                    screenName = "planetlink",
                    name = "PlanetLink",
                ),
                replyCount = 1,
                retweetCount = 2,
                favoriteCount = 3,
                bookmarkCount = 4,
                quoteCount = 5,
                media = listOf(
                    Media(
                        type = "photo",
                        url = "https://pbs.twimg.com/media/image.jpg",
                    )
                ),
                viewCount = 6,
                inReplyToStatusId = "100",
                conversationId = "100",
                lang = "en",
                article = Article(
                    id = "article-1",
                    title = "An article",
                    plainText = "Article body",
                ),
            ),
            service,
        )

        assertIs<XComment>(comment)
        assertEquals("456", comment.id<String>())
        assertEquals("Hello #Kotlin", comment.text?.displayText)
        assertEquals(1704112496000, assertNotNull(comment.createAt).toEpochMilliseconds())
        assertIs<XUser>(comment.user)
        assertEquals(MediaType.Image, comment.medias.single().type)
        assertEquals("100", comment.replyTo?.id<String>())
        assertEquals("An article", comment.article?.title)
        assertEquals("https://x.com/planetlink/status/456", comment.webUrl)
        assertEquals(5, comment.reactions.size)
    }

    @Test
    fun mapsRetweetAsSharedComment() {
        val comment = XMapper.comment(
            Tweet(
                id = "retweet-1",
                text = "RT @original: Original post",
                user = User(
                    id = "retweet-user",
                    screenName = "retweeter",
                    name = "Retweet User",
                ),
                retweetedTweet = Tweet(
                    id = "original-1",
                    text = "Original post",
                    user = User(
                        id = "original-user",
                        screenName = "original",
                        name = "Original User",
                        profileImageUrl = "https://pbs.twimg.com/profile_normal.jpg",
                    ),
                    retweetCount = 3,
                    favoriteCount = 12,
                    media = listOf(
                        Media(
                            type = "photo",
                            url = "https://pbs.twimg.com/media/original.jpg",
                        )
                    ),
                ),
            ),
            service,
        )

        assertEquals(true, comment.isOnlyShared)
        assertEquals("retweet-user", comment.user?.id<String>())
        assertNull(comment.text)
        assertEquals(emptyList(), comment.medias)

        val sharedComment = assertIs<XComment>(comment.sharedComment)
        assertEquals("original-1", sharedComment.id<String>())
        assertEquals("Original post", sharedComment.text?.displayText)
        assertEquals("original-user", sharedComment.user?.id<String>())
        assertEquals("https://pbs.twimg.com/profile_400x400.jpg", sharedComment.user?.iconImageUrl)
        assertEquals(MediaType.Image, sharedComment.medias.single().type)
        assertEquals(12, sharedComment.likeCount)
        assertEquals(3, sharedComment.shareCount)
    }
}
