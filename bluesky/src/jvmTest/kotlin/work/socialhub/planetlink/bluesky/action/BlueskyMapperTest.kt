package work.socialhub.planetlink.bluesky.action

import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewBasic
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedRecord
import work.socialhub.kbsky.model.app.bsky.embed.EmbedRecordWithMedia
import work.socialhub.kbsky.model.app.bsky.embed.EmbedUnion
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideo
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideoView
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsGeneratorView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.app.bsky.graph.GraphDefsListView
import work.socialhub.kbsky.model.share.Blob
import work.socialhub.kbsky.model.share.BlobRef
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import work.socialhub.kbsky.stream.entity.app.bsky.model.Commit
import work.socialhub.kbsky.stream.entity.app.bsky.model.Event
import work.socialhub.planetlink.bluesky.define.BlueskyActionType
import work.socialhub.planetlink.bluesky.model.BlueskyChannel
import work.socialhub.planetlink.bluesky.model.BlueskyComment
import work.socialhub.planetlink.bluesky.model.BlueskyPaging
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueskyMapperTest {

    private val service = Service("bluesky", Account())

    @Test
    fun channel_listView_mapsListMetadataAndPaging() {
        val list = GraphDefsListView(
            uri = "at://did:plc:owner/app.bsky.graph.list/list-key",
            cid = "bafy-list",
            creator = ActorDefsProfileView(
                did = "did:plc:owner",
                handle = "owner.bsky.social",
                displayName = "Owner",
            ),
            name = "Kotlin",
            purpose = "app.bsky.graph.defs#curatelist",
            description = "Kotlin developers",
            avatar = "https://cdn.bsky.app/list.png",
            indexedAt = "2025-01-01T00:00:00.000Z",
        )

        val result = BlueskyMapper.channels(
            listOf(list),
            "next-cursor",
            BlueskyPaging(),
            service,
        )
        val channel = result.entities.single() as BlueskyChannel

        assertEquals(list.uri, channel.id<String>())
        assertEquals(list.cid, channel.cid)
        assertEquals(list.name, channel.name)
        assertEquals(list.description, channel.description)
        assertEquals(list.purpose, channel.purpose)
        assertEquals(list.avatar, channel.iconUrl)
        assertEquals("Owner", channel.owner?.name)
        assertEquals("next-cursor", (result.paging as BlueskyPaging).cursorHint)
    }

    @Test
    fun capabilities_includeCustomFeedApis() {
        assertTrue(
            BlueskyAction.CAPABILITIES.isSupported(
                BlueskyActionType.GetCustomFeeds
            )
        )
        assertTrue(
            BlueskyAction.CAPABILITIES.isSupported(
                BlueskyActionType.CustomFeedTimeLine
            )
        )
    }

    @Test
    fun timelineByPosts_emptyPage_preservesCursor() {
        val result = BlueskyMapper.timelineByPosts(
            emptyList(),
            "next-cursor",
            BlueskyPaging(),
            service,
        )

        assertTrue(result.entities.isEmpty())
        assertEquals(
            "next-cursor",
            (result.paging as BlueskyPaging).cursorHint,
        )
    }

    @Test
    fun customFeeds_latestRecord_returnsNewFeeds() {
        val oldFeed = customFeed(
            "at://did:plc:owner/app.bsky.feed.generator/old",
            "Old feed",
        )
        val newFeed = customFeed(
            "at://did:plc:owner/app.bsky.feed.generator/new",
            "New feed",
        )
        val paging = BlueskyPaging().also {
            it.latestRecord = Identify(service).apply {
                id = ID(oldFeed.uri!!)
            }
        }

        val result = BlueskyMapper.customFeeds(
            listOf(newFeed, oldFeed),
            paging,
            service,
        )

        assertEquals(
            listOf(newFeed.uri),
            result.entities.map { it.id<String>() },
        )
    }

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

    private fun customFeed(
        uri: String,
        name: String,
    ): FeedDefsGeneratorView {
        return FeedDefsGeneratorView().apply {
            this.uri = uri
            cid = "bafy-feed"
            did = "did:web:feed.example"
            creator = ActorDefsProfileView(
                did = "did:plc:owner",
                handle = "owner.bsky.social",
                displayName = "Owner",
            )
            displayName = name
            indexedAt = "2025-01-01T00:00:00.000Z"
        }
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

    @Test
    fun hydrateEventReferences_quote_populatesSharedComment() {
        val quoteUri = "at://did:plc:quoted/app.bsky.feed.post/quoted"
        val event = streamEvent(
            EmbedRecord(record = RepoStrongRef(quoteUri, "bafyquoted")),
        )
        val comment = assertNotNull(BlueskyMapper.commentFromEvent(event, service))
            as BlueskyComment
        val quotedPost = postView(quoteUri, "Quoted post")

        BlueskyMapper.hydrateEventReferences(
            comment,
            event,
            mapOf(quoteUri to quotedPost),
            service,
        )

        assertEquals(listOf(quoteUri), BlueskyMapper.eventReferenceUris(event))
        assertEquals("Quoted post", comment.sharedComment?.text?.displayText)
    }

    @Test
    fun eventReferenceUris_quoteWithMedia_includesQuote() {
        val quoteUri = "at://did:plc:quoted/app.bsky.feed.post/with-media"
        val event = streamEvent(
            EmbedRecordWithMedia(
                record = EmbedRecord(
                    record = RepoStrongRef(quoteUri, "bafyquoted"),
                ),
            ),
        )

        assertEquals(listOf(quoteUri), BlueskyMapper.eventReferenceUris(event))
    }

    @Test
    fun hydrateEventReferences_reply_populatesParentAndRoot() {
        val parentUri = "at://did:plc:parent/app.bsky.feed.post/parent"
        val rootUri = "at://did:plc:root/app.bsky.feed.post/root"
        val event = streamEvent(
            reply = FeedPostReplyRef(
                parent = RepoStrongRef(parentUri, "bafyparent"),
                root = RepoStrongRef(rootUri, "bafyroot"),
            ),
        )
        val comment = assertNotNull(BlueskyMapper.commentFromEvent(event, service))
            as BlueskyComment

        BlueskyMapper.hydrateEventReferences(
            comment,
            event,
            mapOf(
                parentUri to postView(parentUri, "Parent post"),
                rootUri to postView(rootUri, "Root post"),
            ),
            service,
        )

        assertEquals(
            listOf(parentUri, rootUri),
            BlueskyMapper.eventReferenceUris(event),
        )
        assertEquals(
            "Parent post",
            (comment.replyTo as BlueskyComment).text?.displayText,
        )
        assertEquals("Root post", (comment.replyRootTo as BlueskyComment).text?.displayText)
    }

    private fun streamEvent(
        embed: EmbedUnion? = null,
        reply: FeedPostReplyRef? = null,
    ): Event {
        return Event(
            did = "did:plc:author",
            timeUs = 1_700_000_000_000_000L,
            kind = "commit",
            commit = Commit(
                operation = "create",
                collection = "app.bsky.feed.post",
                rkey = "stream",
                cid = "bafystream",
                record = FeedPost().apply {
                    text = "Stream post"
                    this.embed = embed
                    this.reply = reply
                },
            ),
        )
    }

    private fun postView(uri: String, text: String): FeedDefsPostView {
        return FeedDefsPostView().apply {
            this.uri = uri
            cid = "bafyview"
            author = ActorDefsProfileViewBasic(
                did = "did:plc:referenced",
                handle = "referenced.bsky.social",
            )
            indexedAt = "2025-01-01T00:00:00.000Z"
            record = FeedPost().apply { this.text = text }
        }
    }
}
