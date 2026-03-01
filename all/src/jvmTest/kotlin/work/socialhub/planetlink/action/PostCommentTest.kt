package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.MediaForm
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
        fun testTumblr() = runTest {
            tumblr().act(text = "Image", fileData = icon(), fileName = "icon.png")
        }

        @Test
        fun testSlack() = runTest {
            slack().act(text = "Image", fileData = icon(), fileName = "icon.png")
        }
    }
}
