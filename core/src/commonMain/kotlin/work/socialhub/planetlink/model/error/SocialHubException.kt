package net.socialhub.planetlink.model.error

class SocialHubException : java.lang.RuntimeException {
    // region
    // endregion
    /**
     * Error detail
     * エラー情報をまとめたものを設定
     */
    var error: SocialHubError? = null

    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)
}
