package work.socialhub.planetlink.nostr.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import work.socialhub.planetlink.model.request.MediaForm

class NostrMediaUploadMapperTest {

    @Test
    fun mapsMediaFormToNostrUpload() {
        val data = byteArrayOf(1, 2, 3)
        val upload = MediaForm(data, "photo.WEBP").also {
            it.description = "Sunset"
        }.toNostrMediaUpload()

        assertTrue(upload.fileData.contentEquals(data))
        assertEquals("photo.WEBP", upload.fileName)
        assertEquals("image/webp", upload.mimeType)
        assertEquals("Sunset", upload.description)
    }

    @Test
    fun detectsSupportedImageMimeTypes() {
        assertEquals("image/avif", nostrMediaMimeType("photo.avif"))
        assertEquals("image/gif", nostrMediaMimeType("photo.gif"))
        assertEquals("image/heic", nostrMediaMimeType("photo.heic"))
        assertEquals("image/heif", nostrMediaMimeType("photo.heif"))
        assertEquals("image/png", nostrMediaMimeType("photo.png"))
        assertEquals("image/svg+xml", nostrMediaMimeType("photo.svg"))
        assertEquals("image/webp", nostrMediaMimeType("photo.webp"))
        assertEquals("image/jpeg", nostrMediaMimeType("photo.jpg"))
        assertEquals("image/jpeg", nostrMediaMimeType("photo"))
    }
}
