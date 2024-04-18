package work.socialhub.planetlink.micro

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User

open class MicroBlogUser(
    service: Service
) : User(service) {

    var screenName: String? = null
    var profileUrl: String? = null
    var isProtected: Boolean? = null

    var statusesCount: Int? = null
    var followingCount: Int? = null
    var followersCount: Int? = null
}
