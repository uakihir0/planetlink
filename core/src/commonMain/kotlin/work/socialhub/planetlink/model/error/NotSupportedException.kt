package work.socialhub.planetlink.model.error

import kotlin.js.JsExport

@JsExport
class NotSupportedException : SocialHubException {
    @JsExport.Ignore
    constructor() : super()
    @JsExport.Ignore
    constructor(message: String) : super(message)
    @JsExport.Ignore
    constructor(message: String, cause: Throwable) : super(message, cause)
}
