package work.socialhub.planetlink.tumblr.model

import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import kotlin.js.JsExport


@JsExport
class TumblrUser(
    service: Service
) : User(service) {

    /** Count of followers  */
    var followersCount: Int? = null

    /** Count of posts  */
    var postsCount: Int? = null

    /** Count of likes  */
    var likesCount: Int? = null

    /** Blog Title  */
    var blogTitle: String? = null

    /**
     * Relationship
     * (Only following and blocked)
     */
    var relationship: Relationship? = null

    /** Whether the user is muted */
    var isMuted: Boolean = false

    /** Whether the user is blocked */
    var isBlocked: Boolean = false

    override val accountIdentify: String
        get() = id!!.value<String>()
}