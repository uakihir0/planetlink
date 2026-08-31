package work.socialhub.planetlink.bluesky.action

import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewBasic
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedLike
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlueskyNotificationMapperTest {

    private val indexedAt = "2026-01-01T00:00:00.000Z"

    private fun service(): Service {
        val account = Account()
        return Service("Bluesky", account).also { account.service = it }
    }

    private fun postView(uri: String) = FeedDefsPostView(
        uri = uri,
        cid = "cid-$uri",
        author = ActorDefsProfileViewBasic(
            did = "did:plc:author",
            handle = "author.example.com",
        ),
        record = FeedPost(text = "text of $uri", createdAt = indexedAt),
        indexedAt = indexedAt,
    )

    private fun notification(
        uri: String,
        reason: String,
        reasonSubject: String? = null,
        record: work.socialhub.kbsky.model.share.RecordUnion? = null,
    ) = NotificationListNotificationsNotification(
        uri = uri,
        cid = "cid-$uri",
        author = ActorDefsProfileView(
            did = "did:plc:sender",
            handle = "sender.example.com",
        ),
        reason = reason,
        reasonSubject = reasonSubject,
        record = record,
        indexedAt = indexedAt,
    )

    @Test
    fun aReplyNotificationCarriesTheReplyItself() {
        val notification = notification(
            uri = "at://did:plc:sender/app.bsky.feed.post/reply",
            reason = "reply",
            record = FeedPost(text = "a reply", createdAt = indexedAt),
        )

        val result = BlueskyMapper.notifications(
            listOf(notification),
            listOf(postView(notification.uri)),
            null,
            service(),
        )

        val mapped = result.entities.single()
        assertEquals("reply", mapped.type)
        assertEquals("mention", mapped.action)
        assertEquals(
            notification.uri,
            mapped.comments?.single()?.id?.value
        )
    }

    @Test
    fun aQuoteNotificationCarriesTheQuotingPost() {
        val notification = notification(
            uri = "at://did:plc:sender/app.bsky.feed.post/quote",
            reason = "quote",
            reasonSubject = "at://did:plc:me/app.bsky.feed.post/quoted",
            record = FeedPost(text = "a quote", createdAt = indexedAt),
        )

        val result = BlueskyMapper.notifications(
            listOf(notification),
            listOf(postView(notification.uri)),
            null,
            service(),
        )

        val mapped = result.entities.single()
        assertEquals("quote", mapped.type)
        assertEquals("quote", mapped.action)
        assertEquals(
            notification.uri,
            mapped.comments?.single()?.id?.value
        )
    }

    @Test
    fun aLikeNotificationStillCarriesTheLikedPost() {
        val subject = "at://did:plc:me/app.bsky.feed.post/liked"
        val notification = notification(
            uri = "at://did:plc:sender/app.bsky.feed.like/like",
            reason = "like",
            reasonSubject = subject,
            record = FeedLike(
                subject = RepoStrongRef(uri = subject, cid = "cid"),
                createdAt = indexedAt,
            ),
        )

        val result = BlueskyMapper.notifications(
            listOf(notification),
            listOf(postView(subject)),
            null,
            service(),
        )

        val mapped = result.entities.single()
        assertEquals("like", mapped.type)
        assertEquals("like", mapped.action)
        assertEquals(subject, mapped.comments?.single()?.id?.value)
    }

    @Test
    fun aFollowNotificationHasNoTargetPost() {
        val notification = notification(
            uri = "at://did:plc:sender/app.bsky.graph.follow/follow",
            reason = "follow",
        )

        val result = BlueskyMapper.notifications(
            listOf(notification),
            emptyList(),
            null,
            service(),
        )

        val mapped = result.entities.single()
        assertEquals("follow", mapped.action)
        assertNull(mapped.comments)
    }
}
