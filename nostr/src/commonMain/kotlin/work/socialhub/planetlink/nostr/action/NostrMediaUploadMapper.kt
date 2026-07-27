package work.socialhub.planetlink.nostr.action

import work.socialhub.knostr.social.model.NostrMediaUpload
import work.socialhub.planetlink.model.request.MediaForm

internal fun MediaForm.toNostrMediaUpload(): NostrMediaUpload {
    return NostrMediaUpload(
        fileData = data,
        fileName = name,
        mimeType = nostrMediaMimeType(name),
        description = description.orEmpty(),
    )
}

internal fun nostrMediaMimeType(fileName: String?): String {
    return when (fileName?.substringAfterLast('.', "")?.lowercase()) {
        "avif" -> "image/avif"
        "gif" -> "image/gif"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        "png" -> "image/png"
        "svg" -> "image/svg+xml"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
}
