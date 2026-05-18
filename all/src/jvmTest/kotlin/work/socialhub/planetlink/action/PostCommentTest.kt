package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.MediaForm
import work.socialhub.planetlink.matrix.model.MatrixComment
import kotlin.test.Test

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
