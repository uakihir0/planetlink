package work.socialhub.planetlink.model.error

import work.socialhub.planetlink.define.ServiceType
import kotlin.js.JsExport

@JsExport
open class SocialHubException : RuntimeException {

    var error: SocialHubError? = null

    var serviceType: ServiceType? = null

    @JsExport.Ignore
    constructor() : super()

    @JsExport.Ignore
    constructor(message: String?) : super(message)

    @JsExport.Ignore
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    @JsExport.Ignore
    constructor(cause: Throwable?) : super(cause)
}
