package work.socialhub.planetlink.bluesky.action

import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.planetlink.bluesky.model.BlueskyPaging
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
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
    fun profileUpdateRequest_defaultPronounsAreNull() {
        val request = profileUpdateRequest(
            auth = auth,
            form = ProfileForm().apply { displayName = "New name" },
            avatar = null,
            banner = null,
        )

        assertNull(request.pronouns)
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

    @Test
    fun notificationRequest_nullReasonsDoesNotFilter() {
        val request = notificationRequest(
            auth = auth,
            cursor = null,
            reasons = null,
        )

        assertNull(request.cursor)
        assertNull(request.reasons)
    }

    @Test
    fun notificationReasons_usesReasonsForInitialLoad() {
        val types = listOf("reply", "quote")

        assertEquals(types, notificationReasons(Paging(), types))
        assertEquals(types, notificationReasons(BlueskyPaging(), types))
    }

    @Test
    fun notificationReasons_skipsReasonsForRefreshBoundary() {
        val service = Service("bluesky", Account())
        val paging = BlueskyPaging().apply {
            latestRecord = Identify(service).apply {
                id = ID("at://did:plc:test/app.bsky.notification/1")
            }
        }

        assertNull(notificationReasons(paging, listOf("reply", "quote")))
    }

    @Test
    fun truncateNotificationsAtBoundary_keepsNewerNotifications() {
        val notifications = listOf(
            notification("new-1"),
            notification("new-2"),
            notification("boundary"),
            notification("old"),
        )

        val result = truncateNotificationsAtBoundary(notifications, "boundary")

        assertEquals(listOf("new-1", "new-2"), result?.map { it.uri })
    }

    @Test
    fun truncateNotificationsAtBoundary_returnsNullWhenBoundaryIsMissing() {
        val notifications = listOf(
            notification("new-1"),
            notification("new-2"),
        )

        assertNull(truncateNotificationsAtBoundary(notifications, "boundary"))
    }

    private fun notification(uri: String) =
        NotificationListNotificationsNotification(uri = uri)
}
