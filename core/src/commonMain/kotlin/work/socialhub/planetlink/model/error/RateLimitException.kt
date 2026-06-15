package work.socialhub.planetlink.model.error

import work.socialhub.planetlink.define.ErrorType
import kotlin.js.JsExport

@JsExport
class RateLimitException(
    responseBody: String?,
    message: String?,
    cause: Throwable?,
) : HttpException(429, responseBody, message, cause) {

    var retryAfterSeconds: Long? = null

    @JsExport.Ignore
    constructor(responseBody: String?, cause: Throwable?)
        : this(responseBody, "Rate limit exceeded (HTTP 429)", cause)

    init {
        error = ErrorType.RATE_LIMIT_EXCEEDED
    }
}
