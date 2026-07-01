package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.MediaForm
import work.socialhub.planetlink.matrix.model.MatrixComment
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PostCommentTest {

    companion object {
        private suspend fun Account.act(
            text: String = "TEST",
            fileData: ByteArray? = null,
            fileName: String? = null,
        ) {
            action.postComment(
                CommentForm().also {
                    it.text = text

                    // 添付ファイル
                    if (fileData != null && fileName != null) {
                        it.images = mutableListOf(MediaForm(fileData, fileName))
                    }
                })
        }

        suspend fun roomId(account: Account): String {
            val rooms = account.action.channels(
                Identify(account.service, ID("")), Paging(1)
            )
            return rooms.entities.firstOrNull()?.id?.value<String>()
                ?: throw IllegalStateException("No joined rooms found")
        }
    }

    @Nested
    inner class Simple : AbstractTest() {
        @Test
        fun testBluesky() = runTest { bluesky().act() }

        @Test
        fun testMisskey() = runTest { misskey().act() }

        @Test
        fun testMastodon() = runTest { mastodon().act() }

        @Test
        fun testTumblr() = runTest { tumblr().act() }

        @Test
        fun testSlack() = runTest { slack().act() }

        @Test
        fun testNostr() = runTest { nostr().act() }

        @Test
        fun testMatrix() = runTest {
            val account = matrix()
            val rid = roomId(account)
            account.action.postComment(
                CommentForm().also {
                    it.text = "TEST"
                    it.addParam(MatrixComment.ROOM_KEY, rid)
                }
            )
        }
    }

    @Nested
    inner class ScheduledPost : AbstractTest() {
        @Test
        fun testMastodonScheduledPostInvalidFormat() = runTest {
            assertFailsWith<SocialHubException> {
                mastodon().action.postComment(
                    CommentForm().also {
                        it.text = "Scheduled post test"
                        it.scheduledAt = "not-a-valid-date"
                    })
            }
        }

        @Test
        fun testMastodonScheduledPostTooSoon() = runTest {
            // Mastodon requires scheduled_at to be at least 5 minutes in the future.
            // Using plusSeconds(60) intentionally violates this to trigger the
            // SocialHubException from the up-front validation.
            assertFailsWith<SocialHubException> {
                mastodon().action.postComment(
                    CommentForm().also {
                        it.text = "Scheduled post test"
                        it.scheduledAt = Instant.now().plusSeconds(60).toString()
                    })
            }
        }

    }

    @Nested
    inner class WithFile : AbstractTest() {
        @Test
        fun testBluesky() = runTest {
            bluesky().act(text = "Image", fileData = icon(), fileName = "icon.png")
        }

        @Test
        fun testMisskey() = runTest {
            misskey().act(text = "Image", fileData = icon(), fileName = "icon.png")
        }

        @Test
        fun testMastodon() = runTest {
            mastodon().act(text = "Image", fileData = icon(), fileName = "icon.png")
        }

        @Test
        fun testSlack() = runTest {
            slack().act(text = "Image", fileData = icon(), fileName = "icon.png")
        }

        @Test
        fun testNostr() = runTest {
            // Nostr file uploads not supported yet
            nostr().act(text = "Image")
        }

        @Test
        fun testMatrix() = runTest {
            val account = matrix()
            val rid = roomId(account)
            account.action.postComment(
                CommentForm().also {
                    it.text = "Image"
                    it.addParam(MatrixComment.ROOM_KEY, rid)
                }
            )
        }
    }
}
