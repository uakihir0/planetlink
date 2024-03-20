package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Service

class MisskeyNotification(
    service: Service
) : Notification(service) {

    var reaction: String? = null
    var iconUrl: String? = null
}
