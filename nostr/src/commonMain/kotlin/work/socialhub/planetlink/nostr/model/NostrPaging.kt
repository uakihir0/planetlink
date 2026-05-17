package work.socialhub.planetlink.nostr.model

import work.socialhub.planetlink.model.Paging

class NostrPaging : Paging() {

    var since: Long? = null
    var until: Long? = null

    override fun copy(): NostrPaging {
        val p = NostrPaging()
        copyTo(p)
        p.since = since
        p.until = until
        return p
    }

    companion object {
        fun fromPaging(paging: Paging?): NostrPaging {
            val p = NostrPaging()
            if (paging != null) {
                paging.copyTo(p)
                if (paging is NostrPaging) {
                    p.since = paging.since
                    p.until = paging.until
                }
            }
            return p
        }
    }
}
