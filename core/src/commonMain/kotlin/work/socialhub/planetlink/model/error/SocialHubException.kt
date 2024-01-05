package work.socialhub.planetlink.model.error

import net.socialhub.planetlink.model.error.SocialHubError

class SocialHubException : RuntimeException {

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
