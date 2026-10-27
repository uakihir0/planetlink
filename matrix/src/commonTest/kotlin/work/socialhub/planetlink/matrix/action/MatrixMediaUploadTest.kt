package work.socialhub.planetlink.matrix.action

import work.socialhub.planetlink.model.request.MediaForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatrixMediaUploadTest {

    @Test
    fun guessesMimeTypeFromExtension() {
        assertEquals("image/jpeg", MatrixMediaUpload.mimeType("photo.JPG"))
        assertEquals("image/jpeg", MatrixMediaUpload.mimeType("photo.jpeg"))
        assertEquals("image/png", MatrixMediaUpload.mimeType("image0.png"))
        assertEquals("image/gif", MatrixMediaUpload.mimeType("a.gif"))
        assertEquals("video/mp4", MatrixMediaUpload.mimeType("clip.mp4"))
        assertEquals("application/octet-stream", MatrixMediaUpload.mimeType("noext"))
    }

    @Test
    fun buildsImageMessage() {
        val media = MediaForm(byteArrayOf(1, 2, 3), "image0.png").also {
            it.width = 640
            it.height = 480
            it.description = "alt"
        }
        val req = MatrixMediaUpload.imageMessageRequest(
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
    fun imageMessageWithoutSizeOrReply() {
        val media = MediaForm(byteArrayOf(1), "photo.jpg")
        val req = MatrixMediaUpload.imageMessageRequest(
            "!room:example.org", "mxc://example.org/x", media, null,
        )
        assertNull(req.width)
        assertNull(req.height)
        assertNull(req.replyTo)
        assertEquals("image/jpeg", req.mimetype)
    }
}
