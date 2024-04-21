package work.socialhub.planetlink.model.error

open class SocialHubException : RuntimeException {

    /**
     * Error detail
     * エラー情報をまとめたものを設定
     */
    var error: SocialHubError? = null

    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable) : super(cause)
}
