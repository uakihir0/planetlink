package work.socialhub.planetlink.micro

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User

open class MicroBlogUser(
    service: Service
) : User(service) {

    var screenName: String? = null

    var protected: Boolean? = null

    var statusesCount: Long? = null
    var followingCount: Long? = null
    var followersCount: Long? = null
}
