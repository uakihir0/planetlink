package work.socialhub.planetlink.mixi2.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import work.socialhub.kmixi2web.entity.Community
import work.socialhub.kmixi2web.entity.CommunityAccessLevel
import work.socialhub.kmixi2web.entity.LinkCard
import work.socialhub.kmixi2web.entity.Media
import work.socialhub.kmixi2web.entity.Notification
import work.socialhub.kmixi2web.entity.NotificationActivityType
import work.socialhub.kmixi2web.entity.Persona
import work.socialhub.kmixi2web.entity.PersonaConnectivity
import work.socialhub.kmixi2web.entity.PersonaName
import work.socialhub.kmixi2web.entity.Post
import work.socialhub.kmixi2web.entity.PostImage
import work.socialhub.kmixi2web.entity.PostStamp
import work.socialhub.kmixi2web.entity.Profile
import work.socialhub.kmixi2web.entity.Stamp
import work.socialhub.kmixi2web.entity.Timestamp
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.mixi2.model.Mixi2Comment
import work.socialhub.planetlink.mixi2.model.Mixi2User

class Mixi2MapperTest {

    private val service = Service("mixi2", Account())

    @Test
    fun mapsUser() {
        val user = Mixi2Mapper.user(
            Persona(
                personaId = "persona-1",
                name = "planetlink",
                displayName = "PlanetLink",
                avatarUrl = "https://img.mixi.social/avatar.png",
                profileImageUrl = "https://img.mixi.social/cover.png",
                profileText = "Kotlin Multiplatform",
                following = true,
                followed = false,
                verificationType = 1,
                statusText = "hello",
            ),
            service,
        )

        assertNotNull(user)
        assertEquals("persona-1", user.id<String>())
        assertEquals("persona-1", user.personaId)
        assertEquals("planetlink", user.handle)
        assertEquals("PlanetLink", user.name)
        assertEquals("https://img.mixi.social/avatar.png", user.iconImageUrl)
        assertEquals("https://img.mixi.social/cover.png", user.coverImageUrl)
        assertEquals("https://mixi.social/@planetlink", user.webUrl)
        assertEquals(true, user.verified)
    }

    @Test
    fun mapsProfileRelationship() {
        val user = Mixi2Mapper.user(
            Profile(
                persona = Persona(
                    personaId = "persona-1",
                    name = "planetlink",
                    displayName = "PlanetLink",
                ),
                followingCount = 10,
                followedCount = 20,
                text = "profile text",
                link = "https://example.com",
                personaConnectivity = PersonaConnectivity(
                    following = true,
                    followed = true,
                ),
                isMuted = true,
                isBlocking = true,
                isBlocked = false,
            ),
            service,
        )

        assertEquals(10, user.followingCount)
        assertEquals(20, user.followersCount)
        assertEquals("profile text", user.description?.displayText)
        assertEquals("https://example.com", user.link)

        val relationship = assertNotNull(user.relationship)
        assertTrue(relationship.following)
        assertTrue(relationship.followed)
        assertTrue(relationship.blocking)
        assertTrue(relationship.muting)
    }

    @Test
    fun mapsPostWithStampsAndSharedReference() {
        val author = Mixi2Mapper.user(
            Persona(
                personaId = "persona-1",
                name = "planetlink",
                displayName = "PlanetLink",
            ),
            service,
        )
        val users = mapOf("persona-1" to author)

        val comment = Mixi2Mapper.comment(
            Post(
                postId = "post-1",
                createdAt = Timestamp(seconds = 1704112496),
                personaId = "persona-1",
                medias = listOf(
                    Media(
                        mediaId = "media-1",
                        postImage = PostImage(
                            largeImageUrl = "https://img.mixi.social/large.jpg",
                            smallImageUrl = "https://img.mixi.social/small.jpg",
                            largeImageWidth = 1200,
                            largeImageHeight = 800,
                        ),
                    )
                ),
                likesCount = 3,
                repliesCount = 1,
                repostCount = 2,
                quotedCount = 4,
                text = "Hello mixi2",
                liked = true,
                bookmarked = true,
                stamps = listOf(
                    PostStamp(
                        stamp = Stamp(
                            stampId = "stamp-1",
                            url = "https://img.mixi.social/stamp.png",
                        ),
                        count = 5,
                    )
                ),
                readerStampId = "stamp-1",
                isSensitive = true,
                mentions = listOf(PersonaName("persona-2", "other")),
                linkCards = listOf(LinkCard(cardId = "card-1", url = "https://example.com")),
            ),
            users,
            service,
        )

        assertNotNull(comment)
        assertEquals("post-1", comment.id<String>())
        assertEquals("Hello mixi2", comment.text?.displayText)
        assertEquals(1704112496000, assertNotNull(comment.createAt).toEpochMilliseconds())
        assertEquals(author, comment.user)
        assertEquals(MediaType.Image, comment.medias.single().type)
        assertEquals("https://img.mixi.social/large.jpg", comment.medias.single().sourceUrl)
        assertEquals(true, comment.liked)
        assertEquals(true, comment.bookmarked)
        assertEquals(true, comment.possiblySensitive)
        assertEquals(4, comment.quoteCount)
        assertEquals("https://mixi.social/@planetlink/posts/post-1", comment.webUrl)

        val stamp = comment.reactions.first { it.name == "stamp-1" }
        assertEquals(5, stamp.count)
        assertEquals(true, stamp.reacting)
        assertEquals("https://img.mixi.social/stamp.png", stamp.iconUrl)
    }

    @Test
    fun mapsRepostAsSharedComment() {
        val comment = Mixi2Mapper.comment(
            Post(
                postId = "repost-1",
                personaId = "persona-1",
                repostCount = 1,
                reposted = true,
                referencePost = Post(
                    postId = "original-1",
                    personaId = "persona-2",
                    text = "Original post",
                ),
            ),
            mapOf(
                "persona-1" to Mixi2Mapper.user(
                    Persona(personaId = "persona-1", name = "reposter"),
                    service,
                ),
                "persona-2" to Mixi2Mapper.user(
                    Persona(personaId = "persona-2", name = "original"),
                    service,
                ),
            ),
            service,
        )

        assertNotNull(comment)
        assertNull(comment.text)
        assertTrue(comment.isOnlyShared)
        assertEquals("Original post", comment.sharedComment?.text?.displayText)
        assertEquals("original-1", comment.sharedComment?.id<String>())
        assertTrue(comment.reactions.any { it.name == "share" })
    }

    @Test
    fun mapsNotificationAction() {
        val author = Mixi2Mapper.user(
            Persona(personaId = "persona-1", name = "planetlink"),
            service,
        )
        val notification = Mixi2Mapper.notification(
            Notification(
                activityType = NotificationActivityType.LIKE,
                createdAt = Timestamp(seconds = 1704112496),
                timeSeriesId = "ts-1",
                issuerId = "persona-1",
                postId = "post-1",
            ),
            mapOf("persona-1" to author),
            service,
        )

        assertEquals("ts-1", notification.id<String>())
        assertEquals(NotificationActionType.LIKE.code, notification.action)
        assertEquals(1704112496000, assertNotNull(notification.createAt).toEpochMilliseconds())
        assertEquals(author, notification.users?.single())
        assertEquals("post-1", notification.comments?.single()?.id<String>())
    }

    @Test
    fun mapsCommunityAsChannel() {
        val channel = Mixi2Mapper.channel(
            Community(
                communityId = "community-1",
                name = "Kotlin",
                purpose = "A community",
                accessLevel = CommunityAccessLevel.APPROVAL_REQUIRED,
                countOfMembers = 42,
                isArchived = true,
            ),
            service,
        )

        assertEquals("community-1", channel.id<String>())
        assertEquals("Kotlin", channel.name)
        assertEquals("A community", channel.description)
        assertEquals(42, channel.memberCount)
        assertEquals(false, channel.isPublic)
        assertEquals(true, channel.isArchived)
    }

    @Test
    fun mapsUnknownPostAsOtherMedia() {
        val media = Mixi2Mapper.media(Media(mediaId = "media-1"))
        assertEquals(MediaType.Other, media.type)
    }

    @Test
    fun mapsNullTimestampToNull() {
        assertNull(Mixi2Mapper.instant(null))
        assertEquals(
            0,
            assertNotNull(Mixi2Mapper.instant(Timestamp())).toEpochMilliseconds(),
        )
    }

    @Test
    fun mapsCommentType() {
        val comment: Mixi2Comment = Mixi2Mapper.comment(Post(postId = "post-1"), emptyMap(), service)
        assertNull(comment.user)
        assertEquals("post-1", comment.id<String>())
    }

    @Test
    fun mapsUserType() {
        val user: Mixi2User = Mixi2Mapper.user(Persona(personaId = "p1"), service)
        assertEquals("p1", user.accountIdentify)
        assertFalse(user.verified)
    }
}
