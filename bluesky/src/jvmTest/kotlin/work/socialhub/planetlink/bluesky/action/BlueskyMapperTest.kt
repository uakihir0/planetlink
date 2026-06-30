package work.socialhub.planetlink.bluesky.action

import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewBasic
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideo
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideoView
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.share.Blob
import work.socialhub.kbsky.model.share.BlobRef
import work.socialhub.kbsky.stream.entity.app.bsky.model.Commit
import work.socialhub.kbsky.stream.entity.app.bsky.model.Event
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    // ============================================================== //
    // Video / GIF embeds
    // ============================================================== //

    @Test
    fun simpleComment_videoView_mappedAsMovie() {
        // REST path: getPostThread/timeline carry videos (including GIFs) as
        // EmbedVideoView. (https://bsky.app/profile/tmy.bsky.social/post/3mpdelk4i5s2h)
        // Both GIFs and regular videos map to MediaType.Movie, consistent with
        // how Mastodon's gifv is handled.
        val post = FeedDefsPostView().apply {
            uri = "at://did:plc:test/app.bsky.feed.post/abc"
            cid = "bafytest"
            author = ActorDefsProfileViewBasic(
                did = "did:plc:test",
                handle = "tmy.bsky.social",
            )
            indexedAt = "2025-01-01T00:00:00.000Z"
            record = FeedPost().apply { text = "" }
            embed = EmbedVideoView(
                cid = "bafkreivideo",
                playlist = "https://video.bsky.app/watch/did%3Aplc%3Atest/bafkreivideo/playlist.m3u8",
                thumbnail = "https://video.bsky.app/watch/did%3Aplc%3Atest/bafkreivideo/thumbnail.jpg",
                aspectRatio = EmbedDefsAspectRatio(2000, 1366),
            )
        }

        val comment = BlueskyMapper.simpleComment(post, service)

        assertEquals(1, comment.medias.size)
        val media = comment.medias.first()
        assertEquals(MediaType.Movie, media.type)
        assertTrue(media.sourceUrl!!.endsWith("playlist.m3u8"), media.sourceUrl!!)
        assertTrue(media.previewUrl!!.endsWith("thumbnail.jpg"), media.previewUrl!!)
    }

    @Test
    fun simpleComment_plainVideoView_mappedAsMovie() {
        // A normal (non-GIF) video also maps to Movie.
        val post = FeedDefsPostView().apply {
            uri = "at://did:plc:test/app.bsky.feed.post/def"
            cid = "bafytest2"
            author = ActorDefsProfileViewBasic(did = "did:plc:test", handle = "x.bsky.social")
            indexedAt = "2025-01-01T00:00:00.000Z"
            record = FeedPost().apply { text = "" }
            embed = EmbedVideoView(
                cid = "bafkreivideo2",
                playlist = "https://video.bsky.app/watch/did%3Aplc%3Atest/bafkreivideo2/playlist.m3u8",
            )
        }

        val comment = BlueskyMapper.simpleComment(post, service)

        assertEquals(1, comment.medias.size)
        assertEquals(MediaType.Movie, comment.medias.first().type)
    }

    @Test
    fun commentFromEvent_videoRecord_buildsPlayableUrl() {
        // Stream path: JetStream delivers the record-level EmbedVideo (a blob CID),
        // which must be resolved to a video.bsky.app HLS URL, not the image CDN.
        val did = "did:plc:how3test"
        val cid = "bafkreistreamvideo"
        val event = Event(
            did = did,
            timeUs = 1_700_000_000_000_000L,
            kind = "commit",
            commit = Commit(
                operation = "create",
                collection = "app.bsky.feed.post",
                rkey = "abc",
                cid = "bafycommit",
                record = FeedPost().apply {
                    text = ""
                    embed = EmbedVideo(
                        video = Blob(
                            ref = BlobRef(link = cid),
                            mimeType = "video/mp4",
                            size = 29264,
                        ),
                    )
                },
            ),
        )

        val comment = assertNotNull(BlueskyMapper.commentFromEvent(event, service))

        assertEquals(1, comment.medias.size)
        val media = comment.medias.first()
        assertEquals(MediaType.Movie, media.type)
        // DID colons are path-encoded and the URL targets video.bsky.app (HLS), not cdn.bsky.app.
        assertEquals(
            "https://video.bsky.app/watch/did%3Aplc%3Ahow3test/$cid/playlist.m3u8",
            media.sourceUrl,
        )
        assertEquals(
            "https://video.bsky.app/watch/did%3Aplc%3Ahow3test/$cid/thumbnail.jpg",
            media.previewUrl,
        )
    }
}
