package work.socialhub.planetlink.model.error

import kotlin.js.JsExport

@JsExport
class AuthException(
    statusCode: Int,
    responseBody: String?,
    message: String?,
    cause: Throwable?,
) : HttpException(statusCode, responseBody, message, cause) {

    @JsExport.Ignore
    constructor(statusCode: Int, responseBody: String?, cause: Throwable?)
        : this(statusCode, responseBody, "Authentication failed (HTTP $statusCode)", cause)
}
