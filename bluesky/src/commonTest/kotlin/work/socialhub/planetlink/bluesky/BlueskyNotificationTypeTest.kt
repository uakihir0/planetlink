package work.socialhub.planetlink.bluesky

import work.socialhub.planetlink.bluesky.define.BlueskyNotificationType
import work.socialhub.planetlink.define.NotificationActionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlueskyNotificationTypeTest {

    @Test
    fun mentionCoversBothMentionAndReply() {
        assertEquals(
            listOf("mention", "reply"),
            BlueskyNotificationType.codesOf(
                arrayOf(NotificationActionType.MENTION)
            )
        )
    }

    @Test
    fun resolvesTheCodesOfEveryRequestedAction() {
        assertEquals(
            listOf("quote", "follow", "like"),
            BlueskyNotificationType.codesOf(
                arrayOf(
                    NotificationActionType.QUOTE,
                    NotificationActionType.FOLLOW,
                    NotificationActionType.LIKE,
                )
            )
        )
    }

    @Test
    fun resolvesNothingForUnsupportedActions() {
        assertEquals(
            emptyList(),
            BlueskyNotificationType.codesOf(
                arrayOf(
                    NotificationActionType.POLL,
                    NotificationActionType.REACTION,
                )
            )
        )
    }

    @Test
    fun detectsTheReasonsWhoseNotificationIsAPost() {
        assertTrue(BlueskyNotificationType.isPostReason("mention"))
        assertTrue(BlueskyNotificationType.isPostReason("reply"))
        assertTrue(BlueskyNotificationType.isPostReason("quote"))
        assertFalse(BlueskyNotificationType.isPostReason("like"))
        assertFalse(BlueskyNotificationType.isPostReason("repost"))
        assertFalse(BlueskyNotificationType.isPostReason("unknown"))
    }
}
