package work.socialhub.planetlink.bluesky.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlueskyPagingTest {

    @Test
    fun pastPage_promotesCursorHintOnEmptyPage() {
        val paging = BlueskyPaging().also {
            it.cursorHint = "next-cursor"
        }

        val next = paging.pastPage(emptyList<BlueskyComment>()) as BlueskyPaging

        assertEquals("next-cursor", next.cursor)
        assertNull(next.cursorHint)
    }

    @Test
    fun pastPage_emptyPageWithoutHint_keepsCursor() {
        val paging = BlueskyPaging()

        val next = paging.pastPage(emptyList<BlueskyComment>()) as BlueskyPaging

        assertNull(next.cursor)
    }

    @Test
    fun setMarkPagingEnd_keepsPastForEmptyPageWithCursorHint() {
        val paging = BlueskyPaging().also {
            it.cursorHint = "next-cursor"
        }

        paging.setMarkPagingEnd(emptyList<BlueskyComment>())

        assertTrue(paging.isHasPast)
    }
}
