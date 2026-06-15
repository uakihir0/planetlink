package work.socialhub.planetlink.model.error

import kotlin.js.JsExport

@JsExport
class ClientException(
    statusCode: Int,
    responseBody: String?,
    message: String?,
    cause: Throwable?,
) : HttpException(statusCode, responseBody, message, cause) {

    @JsExport.Ignore
    constructor(statusCode: Int, responseBody: String?, cause: Throwable?)
        : this(statusCode, responseBody, "Client error (HTTP $statusCode)", cause)
}
