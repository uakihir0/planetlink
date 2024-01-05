package net.socialhub.planetlink.model.support

import net.socialhub.planetlink.model.Identify
import net.socialhub.planetlink.model.Identify.id
import net.socialhub.planetlink.model.Service

class TupleIdentify(service: Service?) : Identify(service!!) {
    //endregion
    //region // Getter&Setter
    var subId: Any? = null

    /**
     * Identify for Tumble Blog
     * (ブログポストを取得するために必要)
     */
    fun forTumblrBlogIdentify(blogName: String?, postId: String) {
        id = postId
        subId = blogName
    }
}
