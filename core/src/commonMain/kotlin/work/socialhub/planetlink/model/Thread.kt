package net.socialhub.planetlink.model

import work.socialhub.planetlink.model.Service

/**
 * Thread of Group Messaging
 * グループメッセージスレッド
 */
class Thread(service: Service) : Identify(service) {
    //region // Getter&Setter
    /**
     * Attendee
     * 参加者
     */
    var users: List<User>? = null

    /**
     * Last Update Datetime
     * 最終更新日時
     */
    var lastUpdate: java.util.Date? = null

    //endregion
    /**
     * Description about this thread
     * スレッドの簡単な説明文
     */
    var description: String? = null
}
