package work.socialhub.planetlink.bluesky.action

import work.socialhub.kbsky.model.share.Blob
import work.socialhub.kbsky.model.share.BlobRef
import work.socialhub.planetlink.model.request.LinkForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class BlueskyExternalEmbedTest {

    @Test
    fun linkEmbedMapsMetadataAndUploadedThumbnail() {
        val link = LinkForm(
            uri = "https://example.com/article",
            title = "Article",
            description = "Summary",
        )
        val thumbnail = Blob(
            ref = BlobRef(link = "bafkreithumbnail"),
            mimeType = "image/jpeg",
            size = 1234,
        )

        val embed = linkEmbed(link, thumbnail)

        assertEquals("https://example.com/article", embed.external?.uri)
        assertEquals("Article", embed.external?.title)
        assertEquals("Summary", embed.external?.description)
        assertSame(thumbnail, embed.external?.thumb)
    }
}
