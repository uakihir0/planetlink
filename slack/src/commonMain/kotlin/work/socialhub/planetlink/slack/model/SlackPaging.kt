package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Paging

class SlackPaging : Paging() {

    var cursor: String? = null
    var hasMore: Boolean = false

    override fun copy(): SlackPaging {
        val p = SlackPaging()
        copyTo(p)
        p.cursor = cursor
        p.hasMore = hasMore
        return p
    }

    companion object {
        fun fromPaging(paging: Paging?): SlackPaging {
            val p = SlackPaging()
            if (paging != null) {
                paging.copyTo(p)
            }
            return p
        }
    }
}
