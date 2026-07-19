package work.socialhub.planetlink.x.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class XPagingTest {

    @Test
    fun movesToPastCursor() {
        val paging = XPaging(25).also {
            it.currentCursor = "current"
            it.nextCursor = "next"
        }

        val past = assertIs<XPaging>(paging.pastPage(emptyList()))

        assertEquals(25, past.count)
        assertEquals("next", past.currentCursor)
    }

    @Test
    fun refreshesWithoutCursor() {
        val paging = XPaging(25).also {
            it.currentCursor = "current"
            it.nextCursor = "next"
        }

        val newer = assertIs<XPaging>(paging.newPage(emptyList()))

        assertEquals(25, newer.count)
        assertNull(newer.currentCursor)
    }

    @Test
    fun marksEndWhenResponseHasNoCursor() {
        val paging = XPaging(25)

        paging.setMarkPagingEnd(emptyList<Any>())

        assertFalse(paging.isHasNew)
        assertFalse(paging.isHasPast)
    }
}
