package work.socialhub.planetlink.action

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dumpComments
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Nostr relay integration test using runBlocking (real I/O).
 * The existing HomeTimelineTest.testNostr uses runTest (virtual time)
 * which is incompatible with WebSocket I/O, so this test exists separately.
 */
class NostrRelayTest : AbstractTest() {

    @Test
    fun testNostrHomeTimeline() = runBlocking {
        withTimeout(60_000) {
            val account = nostr()
            val result = account.action.homeTimeLine(Paging(20))
            dumpComments(result)
            println("Nostr homeTimeLine: ${result.entities.size} posts")

            assertNotNull(result, "homeTimeLine result should not be null")
            assertNotNull(result.entities, "entities should not be null")

            if (result.entities.isNotEmpty()) {
                result.entities.forEach { comment ->
                    assertNotNull(comment.id, "comment id should not be null")
                    assertNotNull(comment.text, "comment text should not be null")
                }
            }
        }
    }
}
