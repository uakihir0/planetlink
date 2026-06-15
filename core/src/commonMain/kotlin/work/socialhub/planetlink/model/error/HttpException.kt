package work.socialhub.planetlink.model.error

import kotlin.js.JsExport

@JsExport
open class HttpException(
    val statusCode: Int,
    val responseBody: String?,
    message: String?,
    cause: Throwable?,
) : SocialHubException(message, cause) {

    @JsExport.Ignore
    constructor(statusCode: Int, responseBody: String?, cause: Throwable?)
        : this(statusCode, responseBody, "HTTP $statusCode", cause)
}
