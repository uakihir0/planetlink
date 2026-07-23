package work.socialhub.planetlink.model.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class CommentFormLinkTest {

    @Test
    fun copyClonesLinkAndThumbnail() {
        val thumbnail = MediaForm(byteArrayOf(1, 2, 3), "thumbnail.jpg")
        val form = CommentForm().also {
            it.link = LinkForm(
                uri = "https://example.com/article",
                title = "Article",
                description = "Summary",
            ).also { link ->
                link.thumbnail = thumbnail
            }
        }

        val copied = form.copy()

        assertNotSame(form.link, copied.link)
        assertNotSame(form.link?.thumbnail, copied.link?.thumbnail)
        assertEquals("https://example.com/article", copied.link?.uri)
        assertEquals("Article", copied.link?.title)
        assertEquals("Summary", copied.link?.description)
        assertEquals("thumbnail.jpg", copied.link?.thumbnail?.name)
    }
}
