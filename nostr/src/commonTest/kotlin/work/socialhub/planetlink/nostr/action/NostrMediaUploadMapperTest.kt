package work.socialhub.planetlink.nostr.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import work.socialhub.planetlink.model.request.MediaForm

class NostrMediaUploadMapperTest {

    @Test
    fun mapsMediaFormToNostrUpload() {
        val data = byteArrayOf(1, 2, 3)
        val upload = NostrMapper.toNostrMediaUpload(
            MediaForm(data, "photo.WEBP").also {
                it.description = "Sunset"
            }
        )

        assertTrue(upload.fileData.contentEquals(data))
        assertEquals("photo.WEBP", upload.fileName)
        assertEquals("image/webp", upload.mimeType)
        assertEquals("Sunset", upload.description)
    }

    @Test
    fun detectsSupportedImageMimeTypes() {
        assertEquals("image/avif", NostrMapper.nostrMediaMimeType("photo.avif"))
        assertEquals("image/gif", NostrMapper.nostrMediaMimeType("photo.gif"))
        assertEquals("image/heic", NostrMapper.nostrMediaMimeType("photo.heic"))
        assertEquals("image/heif", NostrMapper.nostrMediaMimeType("photo.heif"))
        assertEquals("image/jpeg", NostrMapper.nostrMediaMimeType("photo.jpeg"))
        assertEquals("image/jpeg", NostrMapper.nostrMediaMimeType("photo.JPG"))
        assertEquals("image/png", NostrMapper.nostrMediaMimeType("photo.png"))
        assertEquals("image/svg+xml", NostrMapper.nostrMediaMimeType("photo.svg"))
        assertEquals("image/webp", NostrMapper.nostrMediaMimeType("photo.webp"))
        assertEquals("video/mp4", NostrMapper.nostrMediaMimeType("clip.mp4"))
        assertEquals("audio/mpeg", NostrMapper.nostrMediaMimeType("voice.mp3"))
        assertEquals("application/octet-stream", NostrMapper.nostrMediaMimeType("photo"))
    }
}
