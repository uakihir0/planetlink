package work.socialhub.planetlink.action

import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.MediaForm
import kotlin.test.Test

class PostCommentTest {

    companion object {
        private fun Account.act(
            text: String = "TEST",
            fileData: ByteArray? = null,
            fineName: String? = null,
        ) {
            action.postComment(
                CommentForm().also {
                    it.text = text

                    // 添付ファイル
                    if (fileData != null && fineName != null) {
                        it.images = mutableListOf(MediaForm(fileData, fineName))
                    }
                })
        }
    }

    @Nested
    inner class Simple : AbstractTest() {
        @Test
        fun testBluesky() = bluesky().act()

        @Test
        fun testMisskey() = misskey().act()
    }

    @Nested
    inner class WithFile : AbstractTest() {
        @Test
        fun testBluesky() = bluesky().act(
            text = "Image", fileData = icon(), fineName = "icon.png"
        )

        @Test
        fun testMisskey() = misskey().act(
            text = "Image", fileData = icon(), fineName = "icon.png"
        )
    }
}