package work.socialhub.planetlink.action

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dumpComments
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test
import kotlin.test.assertTrue

class NostrRelayTest : AbstractTest() {

    @Test
    fun testNostrHomeTimeline() = runBlocking {
        withTimeout(60_000) {
            val account = nostr()
            val result = account.action.homeTimeLine(Paging(20))
            dumpComments(result)
            println("Nostr homeTimeLine: ${result.entities.size} posts")
            assertTrue(result.entities.isNotEmpty(), "homeTimeLine should return posts")
        }
    }

    @Test
    fun testNostrUserTimeline() = runBlocking {
        withTimeout(60_000) {
            val account = nostr()
            // fiatjaf - well-known active Nostr user
            val id = Identify(account.service, ID("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"))
            val result = account.action.userCommentTimeLine(id, Paging(10))
            dumpComments(result)
            println("Nostr userTimeLine: ${result.entities.size} posts")
            assertTrue(result.entities.isNotEmpty(), "userCommentTimeLine should return posts for an active user")
        }
    }

    @Test
    fun testNostrNotification() = runBlocking {
        withTimeout(60_000) {
            val account = nostr()
            val result = account.action.notification(Paging(20))
            println("Nostr notification: ${result.entities.size} items")
            result.entities.forEach { n ->
                println("  [${n.action}] ${n.type} from ${n.users?.firstOrNull()?.name} at ${n.createAt}")
            }
            assertTrue(result.entities.isNotEmpty(), "notification should return items")
        }
    }

    @Test
    fun testNostrUserTimelinePaging() = runBlocking {
        withTimeout(60_000) {
            val account = nostr()
            // fiatjaf - well-known active Nostr user (guaranteed to have many posts)
            val id = Identify(account.service, ID("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"))

            val page1 = account.action.userCommentTimeLine(id, Paging(5))
            println("Nostr paging page1: ${page1.entities.size} posts")
            page1.entities.forEach { println("  ${it.createAt} ${it.id}") }
            assertTrue(page1.entities.isNotEmpty(), "page1 should return posts")

            val nextPaging = page1.paging!!.pastPage(page1.entities)
            val np = nextPaging as work.socialhub.planetlink.nostr.model.NostrPaging
            println("Nostr nextPaging: since=${np.since} until=${np.until} count=${np.count}")

            val page2 = account.action.userCommentTimeLine(id, nextPaging)
            println("Nostr paging page2: ${page2.entities.size} posts")
            page2.entities.forEach { println("  ${it.createAt} ${it.id}") }
            assertTrue(page2.entities.isNotEmpty(), "page2 should return posts")

            val page1Ids = page1.entities.map { it.id }
            val page2Ids = page2.entities.map { it.id }
            assertTrue(page1Ids.intersect(page2Ids.toSet()).isEmpty(), "page1 and page2 should not overlap")
        }
    }
}
