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
}
