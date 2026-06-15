package work.socialhub.planetlink.model.error

import kotlin.js.JsExport

@JsExport
class ApplicationException : SocialHubException {
    @JsExport.Ignore
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    @JsExport.Ignore
    constructor(cause: Throwable?) : super(cause)
}
