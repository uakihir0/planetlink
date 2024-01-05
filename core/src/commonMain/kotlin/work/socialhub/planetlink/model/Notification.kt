package net.socialhub.planetlink.model

import net.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Comment

/**
 * Notification
 * 通知
 */
class Notification(service: Service) : Identify(service) {
    // region // Getter&Setter
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
    var createAt: java.util.Date? = null

    /** Associated users  */
    var users: List<User>? = null

    // endregion
    /** Associated comments  */
    var comments: List<Comment>? = null
}
