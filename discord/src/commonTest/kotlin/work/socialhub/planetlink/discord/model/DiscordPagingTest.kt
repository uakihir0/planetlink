package work.socialhub.planetlink.discord.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordPagingTest {

    @Test
    fun marksPastEndForShortPage() {
        val paging = DiscordPaging().apply {
            count = 200
        }

        paging.setMarkPagingEnd(List(42) { Unit })

        assertFalse(paging.isHasPast)
    }

    @Test
    fun keepsPastForFullPage() {
        val paging = DiscordPaging().apply {
            count = 200
        }

        paging.setMarkPagingEnd(List(200) { Unit })

        assertTrue(paging.isHasPast)
    }

    @Test
    fun keepsPastWhenPageSizeIsUnknown() {
        val paging = DiscordPaging()

        paging.setMarkPagingEnd(emptyList<Unit>())

        assertTrue(paging.isHasPast)
    }

    @Test
    fun keepsPastForShortPageWithAfterSet() {
        val paging = DiscordPaging().apply {
            count = 200
            after = "98765"
        }

        paging.setMarkPagingEnd(List(42) { Unit })

        assertTrue(paging.isHasPast)
    }
}
