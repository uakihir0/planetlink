package work.socialhub.planetlink.micro

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service

open class MicroBlogComment(
    service: Service
) : Comment(service) {

    var liked: Boolean = false
    var shared: Boolean = false

    var likeCount: Int? = null
    var shareCount: Int? = null

    var replyTo: Identify? = null
}