package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test

/**
 * Integration tests for the Saypip adapter. They need a `secrets.json` whose `planetlink` block
 * carries `SAYPIP_HOST` and `SAYPIP_ACCESS_TOKEN`; nothing here reaches the deployment without
 * one.
 */
class SaypipTest {

    @Nested
    inner class Me : AbstractTest() {
        @Test
        fun testSaypip() = runTest {
            dump(saypip().action.userMe())
        }
    }

    @Nested
    inner class HomeTimeLine : AbstractTest() {
        @Test
        fun testSaypip() = runTest {
            val page = saypip().action.homeTimeLine(Paging(10))
            println("Home time line count: ${page.entities.size}")
            println("Has past page: ${page.paging?.isHasPast}")
        }
    }

    @Nested
    inner class Notifications : AbstractTest() {
        @Test
        fun testSaypip() = runTest {
            val page = saypip().action.notification(Paging(10))
            println("Notification count: ${page.entities.size}")
            page.entities.forEach { println(it.type) }
        }
    }
}
