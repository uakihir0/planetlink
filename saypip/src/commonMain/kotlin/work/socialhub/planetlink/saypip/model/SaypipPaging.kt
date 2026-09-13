package work.socialhub.planetlink.saypip.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Cursor paging for Saypip.
 *
 * Every paged list here is newest-first and its cursor asks for older rows further down, so
 * [nextCursor] is where an older page is asked for. There is no page further up than the current
 * one: a feed is read from the top.
 */
@JsExport
class SaypipPaging(
    count: Int? = null,
) : Paging(count) {

    /** The cursor this page was asked for with. */
    var cursor: String? = null

    /** The cursor for the next (older) page, or null at the end. */
    var nextCursor: String? = null

    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        return SaypipPaging(count)
    }

    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        return SaypipPaging(count).also {
            it.cursor = nextCursor
            it.isHasPast = nextCursor != null
        }
    }

    override fun setMarkPagingEnd(
        entities: List<*>
    ) {
        isHasNew = false
        if (nextCursor == null) {
            isHasPast = false
        }
    }

    override fun copy(): SaypipPaging {
        return SaypipPaging(count).also {
            it.cursor = cursor
            it.nextCursor = nextCursor
            it.isHasNew = isHasNew
            it.isHasPast = isHasPast
        }
    }

    companion object {
        fun fromPaging(paging: Paging?): SaypipPaging {
            if (paging is SaypipPaging) {
                return paging.copy()
            }
            return SaypipPaging(paging?.count)
        }
    }
}
