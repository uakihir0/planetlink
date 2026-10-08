package work.socialhub.planetlink.mixi2.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class Mixi2PagingTest {

    @Test
    fun movesToPastCursor() {
        val paging = Mixi2Paging(25).also {
            it.cursor = "current"
            it.nextCursor = "next"
        }

        val past = assertIs<Mixi2Paging>(paging.pastPage(emptyList()))

        assertEquals(25, past.count)
        assertEquals("next", past.cursor)
    }

    @Test
    fun refreshesWithoutCursor() {
        val paging = Mixi2Paging(25).also {
            it.cursor = "current"
            it.nextCursor = "next"
        }

        val newer = assertIs<Mixi2Paging>(paging.newPage(emptyList()))

        assertEquals(25, newer.count)
        assertNull(newer.cursor)
    }

    @Test
    fun marksEndWhenResponseHasNoCursor() {
        val paging = Mixi2Paging(25)

        paging.setMarkPagingEnd(emptyList<Any>())

        assertFalse(paging.isHasNew)
        assertFalse(paging.isHasPast)
    }

    @Test
    fun hasPastWhenNextCursorExists() {
        val paging = Mixi2Paging(25).also { it.nextCursor = "next" }

        paging.setMarkPagingEnd(emptyList<Any>())

        assertFalse(paging.isHasNew)
        assertEquals(true, paging.isHasPast)
    }

    @Test
    fun copiesCursorState() {
        val paging = Mixi2Paging(10).also {
            it.cursor = "current"
            it.nextCursor = "next"
        }

        val copy = assertIs<Mixi2Paging>(Mixi2Paging.fromPaging(paging))

        assertEquals("current", copy.cursor)
        assertEquals("next", copy.nextCursor)
        assertEquals(10, copy.count)
        assertIs<Mixi2Paging>(Mixi2Paging.fromPaging(null))
    }
}
