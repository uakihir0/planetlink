package work.socialhub.planetlink.nostr

import kotlinx.coroutines.runBlocking
import work.socialhub.planetlink.nostr.action.NostrAction
import work.socialhub.planetlink.nostr.action.NostrAuth
import work.socialhub.planetlink.nostr.model.NostrPaging
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NostrTimelineTest {

    private lateinit var action: NostrAction

    @BeforeTest
    fun setup() {
        val relays = listOf(
            "wss://nos.lol",
            "wss://relay.damus.io",
            "wss://relay.nostr.band",
            "wss://relay.snort.social",
        )
        val nsec = System.getenv("NOSTR_NSEC")
            ?: System.getProperty("NOSTR_NSEC")
            ?: "nsec15xev848976sm9s75uhm2rvkr6njldgdjc02wta4pktpafe0k5xeqp8hxk9"

        val auth = NostrAuth(relays = relays, nsec = nsec)
        val account = auth.accountWithPrivateKey()
        action = account.action as NostrAction
    }

    @Test
    fun testHomeTimelineHasUserProfiles() = runBlocking {
        val paging = NostrPaging().apply { count = 20 }
        val result = action.homeTimeLine(paging)

        println("Home timeline: ${result.entities.size} comments")
        result.entities.forEach { comment ->
            val user = comment.user
            println("  user=${user?.name ?: "NULL"} icon=${user?.iconImageUrl?.take(40) ?: "none"} text=${comment.text?.displayText?.take(30)}")
        }

        assertTrue(result.entities.isNotEmpty(), "Home timeline should have comments")
        result.entities.forEach { comment ->
            assertNotNull(comment.user, "Every comment should have a user: ${comment.id}")
            assertNotNull(comment.user!!.name, "User should have a name")
        }
    }

    @Test
    fun testPagingWorks() = runBlocking {
        val paging = NostrPaging().apply { count = 10 }
        val firstPage = action.homeTimeLine(paging)

        println("First page: ${firstPage.entities.size} comments")
        if (firstPage.entities.isEmpty()) {
            println("  (empty — skipping pagination test)")
            return@runBlocking
        }

        val pastPaging = firstPage.pastPage()
        assertTrue(pastPaging is NostrPaging, "pastPage should return NostrPaging")
        val np = pastPaging as NostrPaging
        assertNotNull(np.until, "pastPage should set until")
        println("  pastPage until=${np.until}")

        val secondPage = action.homeTimeLine(pastPaging)
        println("Second page: ${secondPage.entities.size} comments")

        if (firstPage.entities.isNotEmpty() && secondPage.entities.isNotEmpty()) {
            val firstOldest = firstPage.entities.last().createAt
            val secondNewest = secondPage.entities.first().createAt
            println("  firstPage oldest=$firstOldest, secondPage newest=$secondNewest")
            assertTrue(
                secondNewest!! <= firstOldest!!,
                "Second page should be older than first page"
            )
        }

        secondPage.entities.forEach { comment ->
            assertNotNull(comment.user, "Second page comments should also have users")
        }
    }
}
