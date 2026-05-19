package work.socialhub.planetlink.model.error

import kotlin.js.JsExport

@JsExport
open class SocialHubException : RuntimeException {

    /**
     * Error detail
     * エラー情報をまとめたものを設定
     */
    var error: SocialHubError? = null

    @JsExport.Ignore
    constructor() : super()

    @JsExport.Ignore
    constructor(message: String) : super(message)

    @JsExport.Ignore
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    @JsExport.Ignore
    constructor(cause: Throwable) : super(cause)
}
