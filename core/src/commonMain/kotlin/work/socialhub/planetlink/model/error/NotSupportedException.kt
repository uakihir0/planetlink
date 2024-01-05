package net.socialhub.planetlink.model.error

import work.socialhub.planetlink.model.error.SocialHubException

class NotSupportedException : SocialHubException {
    constructor() : super()

    constructor(message: String?) : super(message)
}
