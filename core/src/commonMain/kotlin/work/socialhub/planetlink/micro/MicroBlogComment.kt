package work.socialhub.planetlink.micro

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Service

open class MicroBlogComment(
    service: Service
) : Comment(service) {

    var liked: Boolean? = null
    var shared: Boolean? = null

    var likeCount: Long? = null
    var shareCount: Long? = null
}