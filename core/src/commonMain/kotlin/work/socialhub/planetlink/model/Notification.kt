package work.socialhub.planetlink.model

import net.socialhub.planetlink.define.NotificationActionType
import net.socialhub.planetlink.model.Identify
import net.socialhub.planetlink.model.Instance
import net.socialhub.planetlink.model.User

/**
 * Notification
 * 通知
 */
class Notification(
    service: Service
) : Identify(service) {

    /**
     * Notification type name
     * Origin name form social media.
     */
    var type: String? = null

    /**
     * Common Action type name
     * SocialHub common action name.
     *
     * @see NotificationActionType
     */
    var action: String? = null

    /** Date of created  */
    var createAt: Instance? = null

    /** Associated users  */
    var users: List<User>? = null

    /** Associated comments  */
    var comments: List<Comment>? = null
}
