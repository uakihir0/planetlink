package work.socialhub.planetlink.model

import kotlinx.datetime.Instant

/**
 * Thread of Group Messaging
 * グループメッセージスレッド
 */
class Thread(
    service: Service
) : Identify(service) {

    /**
     * Attendee
     * 参加者
     */
    var users: List<User>? = null

    /**
     * Last Update Datetime
     * 最終更新日時
     */
    var lastUpdate: Instant? = null

    /**
     * Description about this thread
     * スレッドの簡単な説明文
     */
    var description: String? = null
}
