package work.socialhub.planetlink.matrix.action

import work.socialhub.kmatrix.api.request.rooms.RoomsSendMessageRequest
import work.socialhub.planetlink.model.request.MediaForm

/**
 * Helpers for sending images to a Matrix room
 * (upload to the content repository, then an `m.image` event).
 */
internal object MatrixMediaUpload {

    /**
     * MIME type guessed from the file extension.
     * kmatrix (khttpclient) sends the upload as a raw body with this type
     * for the common image / video extensions.
     */
    fun mimeType(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "svg" -> "image/svg+xml"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
    }

    /**
     * `m.image` event for an uploaded [contentUri].
     * `body` and `filename` are both the file name: since Matrix 1.10 a
     * `body` that differs from `filename` is read as a caption, and the text
     * of the form is already sent as its own `m.text` event.
     */
    fun imageMessageRequest(
        roomId: String,
        contentUri: String,
        media: MediaForm,
        replyTo: String?,
    ): RoomsSendMessageRequest {
        return RoomsSendMessageRequest().apply {
            this.roomId = roomId
            msgtype = "m.image"
            body = media.name
            filename = media.name
            url = contentUri
            mimetype = mimeType(media.name)
            width = media.width?.toLong()
            height = media.height?.toLong()
            this.replyTo = replyTo
        }
    }
}
