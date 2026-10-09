package work.socialhub.planetlink.mixi2.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Cursor paging for mixi2.
 *
 * Every paged list reads newest-first: an opaque cursor (a feed cursor, a follow
 * cursor, a notification time-series id, or a chat message id) asks for the rows
 * further down, so [nextCursor] is where the older page is asked for. There is no
 * page further up than the current one.
 */
@JsExport
class Mixi2Paging(
    count: Int? = null,
) : Paging(count) {

    /** The cursor this page was asked for with. */
    var cursor: String? = null

    /** The cursor for the next (older) page, or null at the end. */
    var nextCursor: String? = null

    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        return Mixi2Paging(count)
    }

    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        return Mixi2Paging(count).also {
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

    override fun copy(): Mixi2Paging {
        return Mixi2Paging(count).also {
            it.cursor = cursor
            it.nextCursor = nextCursor
            it.isHasNew = isHasNew
            it.isHasPast = isHasPast
        }
    }

    companion object {
        fun fromPaging(paging: Paging?): Mixi2Paging {
            if (paging is Mixi2Paging) {
                return paging.copy()
            }
            return Mixi2Paging(paging?.count)
        }
    }
}
