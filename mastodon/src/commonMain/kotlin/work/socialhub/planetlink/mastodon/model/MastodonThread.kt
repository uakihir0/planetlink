package work.socialhub.planetlink.mastodon.model

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread

class MastodonThread(
    service: Service
) : Thread(service) {

    /** 最後のコメント */
    var lastComment: Comment? = null
}
