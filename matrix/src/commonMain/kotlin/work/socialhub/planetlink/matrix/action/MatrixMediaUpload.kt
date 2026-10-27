package work.socialhub.planetlink.matrix.action

import work.socialhub.kmatrix.api.request.rooms.RoomsSendMessageRequest
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.request.MediaForm

/**
 * Helpers for sending media to a Matrix room
 * (upload to the content repository, then an `m.image` / `m.video` event).
 */
internal object MatrixMediaUpload {

    /**
     * MIME type guessed from the file extension, for the extensions that
     * kmatrix (khttpclient) uploads as a raw body. khttpclient sends any
     * other extension as multipart/form-data, which the Matrix upload
     * endpoint would store as-is (a broken file), so those are rejected by
     * [checkUploadable] and return null here.
     */
    fun mimeType(name: String): String? {
        return when (extension(name)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mpeg" -> "video/mpeg"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            else -> null
        }
    }

    /**
     * Throws [NotSupportedException] for a file that cannot be uploaded
     * correctly (see [mimeType]).
     */
    fun checkUploadable(name: String) {
        if (mimeType(name) == null) {
            throw NotSupportedException(
                "Matrix upload supports only jpg, jpeg, png, gif, mpeg, mp4, mov and webm files: $name"
            )
        }
    }

    /**
     * `m.image` (or `m.video` for a video file) event for an uploaded
     * [contentUri]. `filename` is the file name. `body` is the media
     * description when there is one: since Matrix 1.10 a `body` that
     * differs from `filename` is shown as a caption. Otherwise `body` is
     * the file name too (no caption; the text of the form is sent as its
     * own `m.text` event).
     */
    fun mediaMessageRequest(
        roomId: String,
        contentUri: String,
        media: MediaForm,
        replyTo: String?,
    ): RoomsSendMessageRequest {
        val mimeType = mimeType(media.name)
        return RoomsSendMessageRequest().apply {
            this.roomId = roomId
            msgtype = if (mimeType?.startsWith("video/") == true) "m.video" else "m.image"
            body = media.description?.takeIf { it.isNotBlank() } ?: media.name
            filename = media.name
            url = contentUri
            mimetype = mimeType
            width = media.width?.toLong()
            height = media.height?.toLong()
            this.replyTo = replyTo
        }
    }

    private fun extension(name: String): String =
        name.substringAfterLast('.', "").lowercase()
}
