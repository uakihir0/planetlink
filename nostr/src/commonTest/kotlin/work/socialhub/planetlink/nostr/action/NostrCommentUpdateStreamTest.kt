package work.socialhub.planetlink.nostr.action

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.knostr.social.api.EnrichmentResource
import work.socialhub.knostr.social.model.NostrNoteStats
import work.socialhub.knostr.social.model.NostrUser as KnostrUser
import work.socialhub.knostr.social.model.SocialDataBatch
import work.socialhub.knostr.social.model.SocialDataRequest
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.nostr.model.NostrComment
import work.socialhub.planetlink.nostr.model.NostrUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class NostrCommentUpdateStreamTest {

    @Test
    fun emitsCompleteCopyForDeferredUserAndStats() = runTest {
        val account = Account()
        val service = Service("nostr", account)
        account.service = service
        val enrichment = FakeEnrichment()
        val update = CompletableDeferred<NostrComment>()
        val callback = object : UpdateCommentCallback {
            override fun onUpdate(event: CommentEvent?) {
                update.complete(event!!.comment as NostrComment)
            }
        }
        val original = NostrComment(service).also { comment ->
            comment.id = ID("event-1")
            comment.eventId = "note1event"
            comment.user = NostrUser(service).also {
                it.id = ID("pubkey-1")
                it.name = "pubkey-"
            }
            comment.reactions = listOf(Reaction().also {
                it.name = "like"
                it.count = 1
                it.reacting = true
            })
        }
        val stream = NostrCommentUpdateStream(
            enrichment = enrichment,
            dispatcher = NostrEnrichmentDispatcher(enrichment),
            callback = callback,
            service = service,
            userMe = null,
        )

        stream.addComments(listOf(original))
        stream.open()
        assertTrue(
            enrichment.requests.single().noteStatsEventIds.contains("event-1")
        )

        enrichment.emit(
            SocialDataBatch(
                users = listOf(KnostrUser().also {
                    it.pubkey = "pubkey-1"
                    it.name = "Alice"
                }),
                noteStats = listOf(
                    NostrNoteStats(
                        eventId = "event-1",
                        likeCount = 5,
                        replyCount = 2,
                        repostCount = 3,
                    )
                ),
            )
        )

        val updated = withContext(Dispatchers.Default) {
            withTimeout(2_000) { update.await() }
        }
        assertNotSame(original, updated)
        assertEquals("Alice", updated.user?.name)
        assertEquals(5, updated.likeCount)
        assertEquals(2, updated.replyCount)
        assertEquals(3, updated.repostCount)
        assertEquals(5, updated.reactions.first { it.name == "like" }.count)
        assertTrue(updated.reactions.first { it.name == "like" }.reacting)
        assertEquals("pubkey-", original.user?.name)
        assertEquals(1, original.reactions.first { it.name == "like" }.count)
        stream.close()
    }

    private class FakeEnrichment : EnrichmentResource {
        override var onUpdateCallback: ((SocialDataBatch) -> Unit)? = null
        val requests = mutableListOf<SocialDataRequest>()

        override fun request(request: SocialDataRequest, forceRefresh: Boolean) {
            requests.add(request)
        }

        override suspend fun cancelPending() = Unit

        override fun cancelPendingBlocking() = Unit

        override fun close() {
            onUpdateCallback = null
        }

        fun emit(batch: SocialDataBatch) {
            onUpdateCallback?.invoke(batch)
        }
    }
}
