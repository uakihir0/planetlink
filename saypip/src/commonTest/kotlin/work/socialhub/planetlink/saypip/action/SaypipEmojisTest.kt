package work.socialhub.planetlink.saypip.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaypipEmojisTest {

    private fun emojis() = SaypipAuth()
        .accountWithAccessToken("token", "refresh")
        .action
        .emojis()
        .map { it.emoji }

    @Test
    fun offersThePicturesItsOwnPickerOffers() {
        val emojis = emojis()

        // Saypip's reaction shortlist, not the core catalogue: the server takes
        // any single picture by shape, but the product draws this set.
        assertEquals(24, emojis.size)
        assertTrue("👍" in emojis)
        assertTrue("❤️" in emojis)
        // Deliberately without a thumb down, as Saypip's own list is.
        assertFalse("👎" in emojis)
        assertFalse("🍜" in emojis)
    }
}
