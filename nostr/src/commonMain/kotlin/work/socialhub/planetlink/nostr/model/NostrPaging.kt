package work.socialhub.planetlink.nostr.model

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import kotlin.js.JsExport

@JsExport
class NostrPaging : Paging() {

    var since: Long? = null
    var until: Long? = null

    override fun <T : Identify> newPage(entities: List<T>): Paging {
        val pg = copy()
        if (entities.isNotEmpty()) {
            val newest = entities.firstNotNullOfOrNull { (it as? Comment)?.createAt }
            if (newest != null) {
                pg.since = newest.epochSeconds + 1
                pg.until = null
            }
        }
        return pg
    }

    override fun <T : Identify> pastPage(entities: List<T>): Paging {
        val pg = copy()
        if (entities.isNotEmpty()) {
            val oldest = entities.mapNotNull { (it as? Comment)?.createAt }.minOrNull()
            if (oldest != null) {
                pg.until = oldest.epochSeconds - 1
                pg.since = null
            }
        }
        return pg
    }

    override fun setMarkPagingEnd(entities: List<*>) {
        if (isHasPast && entities.isEmpty() && (count ?: 0) > 0) {
            isHasPast = false
        }
    }

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
