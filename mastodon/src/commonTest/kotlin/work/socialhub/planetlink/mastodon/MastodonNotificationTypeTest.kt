package work.socialhub.planetlink.mastodon

import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.mastodon.define.MastodonNotificationType
import kotlin.test.Test
import kotlin.test.assertEquals

class MastodonNotificationTypeTest {

    @Test
    fun mentionCoversRepliesAsWell() {
        assertEquals(
            listOf("mention"),
            MastodonNotificationType.codesOf(
                arrayOf(NotificationActionType.MENTION)
            )
        )
    }

    @Test
    fun resolvesTheCodesOfEveryRequestedAction() {
        assertEquals(
            listOf("follow", "favourite", "reblog"),
            MastodonNotificationType.codesOf(
                arrayOf(
                    NotificationActionType.FOLLOW,
                    NotificationActionType.LIKE,
                    NotificationActionType.SHARE,
                )
            )
        )
    }

    @Test
    fun resolvesNothingForQuotesWhichMastodonDoesNotNotify() {
        assertEquals(
            emptyList(),
            MastodonNotificationType.codesOf(
                arrayOf(NotificationActionType.QUOTE)
            )
        )
    }
}
