package work.socialhub.planetlink.bluesky.model

import work.socialhub.planetlink.micro.MicroBlogUser
import work.socialhub.planetlink.model.Service

/**
 * Bluesky User Model
 * Bluesky のユーザー情報
 */
class BlueskyUser(
    service: Service
) : MicroBlogUser(service) {

    /**
     * Is simple User object?
     * 簡易ユーザー情報か？
     * (一部の情報が抜けています)
     */
    var isSimple: Boolean = false

    /**
     * Follow Record Uri
     * フォローのレコードの URI
     * (フォローしている場合のみ記録される)
     */
    var followRecordUri: String? = null

    var followedRecordUri: String? = null

    var blockingRecordUri: String? = null

    var muted: Boolean? = null

    var blockedBy: Boolean? = null

    override var name: String = ""
        get() = field.ifEmpty {
            return screenName!!
                .also { field = it }
        }

    override val accountIdentify
        get() = "@$screenName"

    // TODO: HOST名変更
    override var webUrl: String = ""
        get() = field.ifEmpty {
            return "https://bsky.app/profile/$screenName"
                .also { field = it }
        }
}
