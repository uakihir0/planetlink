package work.socialhub.planetlink.model.error

class NotSupportedException : SocialHubException {
    constructor() : super()
    constructor(message: String?) : super(message)
}
