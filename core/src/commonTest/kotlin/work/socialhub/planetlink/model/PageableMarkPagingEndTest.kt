package work.socialhub.planetlink.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageableMarkPagingEndTest {

    @Test
    fun marksTheEndWhenAnEmptyPageIsSetWithoutACount() {
        val paging = Paging()
        val pageable = Pageable<Notification>()
            .also { it.paging = paging }

        assertEquals(0, pageable.entities.size)
        assertFalse(paging.isHasPast)
    }

    @Test
    fun keepsPastWhenNoCountIsRequested() {
        val paging = Paging(0)
        Pageable<Notification>().also { it.paging = paging }

        assertTrue(paging.isHasPast)
    }
}
