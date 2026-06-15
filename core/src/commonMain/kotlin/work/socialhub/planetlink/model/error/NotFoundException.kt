package work.socialhub.planetlink.model.error

import kotlin.js.JsExport

@JsExport
class NotFoundException(
    responseBody: String?,
    message: String?,
    cause: Throwable?,
) : HttpException(404, responseBody, message, cause) {

    @JsExport.Ignore
    constructor(responseBody: String?, cause: Throwable?)
        : this(responseBody, "Resource not found (HTTP 404)", cause)
}
