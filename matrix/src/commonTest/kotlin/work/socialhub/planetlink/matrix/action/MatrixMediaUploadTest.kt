package work.socialhub.planetlink.matrix.action

import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.request.MediaForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MatrixMediaUploadTest {

    @Test
    fun guessesMimeTypeFromExtension() {
        assertEquals("image/jpeg", MatrixMediaUpload.mimeType("photo.JPG"))
        assertEquals("image/jpeg", MatrixMediaUpload.mimeType("photo.jpeg"))
        assertEquals("image/png", MatrixMediaUpload.mimeType("image0.png"))
        assertEquals("image/gif", MatrixMediaUpload.mimeType("a.gif"))
        assertEquals("video/mp4", MatrixMediaUpload.mimeType("clip.mp4"))
        assertNull(MatrixMediaUpload.mimeType("photo.heic"))
        assertNull(MatrixMediaUpload.mimeType("noext"))
    }

    @Test
    fun rejectsFilesSentAsMultipart() {
        listOf("a.webp", "a.heic", "a.heif", "a.svg", "noext").forEach { name ->
            assertFailsWith<NotSupportedException>(name) {
                MatrixMediaUpload.checkUploadable(name)
            }
        }
        listOf("a.jpg", "a.png", "a.gif", "a.mp4", "a.mov", "a.webm").forEach { name ->
            MatrixMediaUpload.checkUploadable(name)
        }
    }

    @Test
    fun buildsImageMessage() {
        val media = MediaForm(byteArrayOf(1, 2, 3), "image0.png").also {
            it.width = 640
            it.height = 480
        }
        val req = MatrixMediaUpload.mediaMessageRequest(
            "!room:example.org",
            "mxc://example.org/abc",
            media,
            "\$reply",
        )

        assertEquals("!room:example.org", req.roomId)
        assertEquals("m.image", req.msgtype)
        assertEquals("mxc://example.org/abc", req.url)
        assertEquals("image0.png", req.body)
        assertEquals("image0.png", req.filename)
        assertEquals("image/png", req.mimetype)
        assertEquals(640L, req.width)
        assertEquals(480L, req.height)
        assertEquals("\$reply", req.replyTo)
    }

    @Test
    fun usesDescriptionAsCaption() {
        val media = MediaForm(byteArrayOf(1), "photo.jpg").also {
            it.description = "a cat"
        }
        val req = MatrixMediaUpload.mediaMessageRequest(
            "!room:example.org", "mxc://example.org/x", media, null,
        )
        assertEquals("a cat", req.body)
        assertEquals("photo.jpg", req.filename)
    }

    @Test
    fun buildsVideoMessage() {
        val media = MediaForm(byteArrayOf(1), "clip.mov")
        val req = MatrixMediaUpload.mediaMessageRequest(
            "!room:example.org", "mxc://example.org/v", media, null,
        )
        assertEquals("m.video", req.msgtype)
        assertEquals("video/quicktime", req.mimetype)
    }

    @Test
    fun imageMessageWithoutSizeOrReply() {
        val media = MediaForm(byteArrayOf(1), "photo.jpg")
        val req = MatrixMediaUpload.mediaMessageRequest(
            "!room:example.org", "mxc://example.org/x", media, null,
        )
        assertNull(req.width)
        assertNull(req.height)
        assertNull(req.replyTo)
        assertEquals("image/jpeg", req.mimetype)
    }
}
