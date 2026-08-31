package work.socialhub.planetlink.misskey

import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.misskey.define.MisskeyNotificationType
import kotlin.test.Test
import kotlin.test.assertEquals

class MisskeyNotificationTypeTest {

    @Test
    fun mentionCoversBothMentionAndReply() {
        assertEquals(
            listOf("mention", "reply"),
            MisskeyNotificationType.codesOf(
                arrayOf(NotificationActionType.MENTION)
            )
        )
    }

    @Test
    fun resolvesTheCodesOfEveryRequestedAction() {
        assertEquals(
            listOf("follow", "renote", "reaction", "quote", "pollEnded"),
            MisskeyNotificationType.codesOf(
                arrayOf(
                    NotificationActionType.FOLLOW,
                    NotificationActionType.SHARE,
                    NotificationActionType.REACTION,
                    NotificationActionType.QUOTE,
                    NotificationActionType.POLL,
                )
            )
        )
    }

    @Test
    fun resolvesNothingForLikesWhichMisskeyExposesAsReactions() {
        assertEquals(
            emptyList(),
            MisskeyNotificationType.codesOf(
                arrayOf(NotificationActionType.LIKE)
            )
        )
    }
}
