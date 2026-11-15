package work.socialhub.planetlink.bluesky.action

import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.planetlink.model.request.ProfileForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlueskyRequestBuilderTest {

    private val auth = BearerTokenAuthProvider("dummy")

    @Test
    fun profileUpdateRequest_mapsPronouns() {
        val form = ProfileForm().apply {
            displayName = "New name"
            pronouns = "they/them"
        }

        val request = profileUpdateRequest(
            auth = auth,
            form = form,
            avatar = null,
            banner = null,
        )

        assertEquals("New name", request.displayName)
        assertEquals("they/them", request.pronouns)
        assertNull(request.description)
    }

    @Test
    fun profileUpdateRequest_passesEmptyPronounsToClear() {
        val form = ProfileForm().apply {
            pronouns = ""
        }

        val request = profileUpdateRequest(
            auth = auth,
            form = form,
            avatar = null,
            banner = null,
        )

        assertEquals("", request.pronouns)
    }

    @Test
    fun notificationRequest_setsReasonsAndPaging() {
        val request = notificationRequest(
            auth = auth,
            cursor = "cursor-1",
            reasons = listOf("reply", "quote", "mention"),
        )

        assertEquals("cursor-1", request.cursor)
        assertEquals(100, request.limit)
        assertEquals(listOf("reply", "quote", "mention"), request.reasons)
    }
}
