package work.socialhub.planetlink.bluesky.action

import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewBasic
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals

class BlueskyMapperTest {

    private val service = Service("bluesky", Account())

    @Test
    fun userDetailed_nullDisplayName_fallbackToHandle() {
        val account = ActorDefsProfileViewDetailed().apply {
            did = "did:plc:test123"
            handle = "xenobladerx98.bsky.social"
            displayName = null
        }

        val user = BlueskyMapper.user(account, service)

        assertEquals("xenobladerx98.bsky.social", user.name)
    }

    @Test
    fun userDetailed_withDisplayName_usesDisplayName() {
        val account = ActorDefsProfileViewDetailed().apply {
            did = "did:plc:test456"
            handle = "someone.bsky.social"
            displayName = "Someone"
        }

        val user = BlueskyMapper.user(account, service)

        assertEquals("Someone", user.name)
    }

    @Test
    fun simpleComment_nullDisplayName_fallbackToHandle() {
        val post = FeedDefsPostView().apply {
            uri = "at://did:plc:test123/app.bsky.feed.post/abc"
            cid = "bafytest"
            author = ActorDefsProfileViewBasic(
                did = "did:plc:test123",
                handle = "xenobladerx98.bsky.social",
                displayName = null,
            )
            indexedAt = "2025-01-01T00:00:00.000Z"
            record = FeedPost().apply { text = "hello" }
        }

        val comment = BlueskyMapper.simpleComment(post, service)

        assertEquals("xenobladerx98.bsky.social", comment.user!!.name)
    }
}
