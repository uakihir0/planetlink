package work.socialhub.planetlink.x.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

@JsExport
class XPaging(
    count: Int? = null,
) : Paging(count) {

    var currentCursor: String? = null
    var nextCursor: String? = null

    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        return XPaging(count)
    }

    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        return XPaging(count).also {
            it.currentCursor = nextCursor
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

    override fun copy(): XPaging {
        return XPaging(count).also {
            it.currentCursor = currentCursor
            it.nextCursor = nextCursor
            it.isHasNew = isHasNew
            it.isHasPast = isHasPast
        }
    }

    companion object {
        fun fromPaging(paging: Paging?): XPaging {
            if (paging is XPaging) {
                return paging.copy()
            }
            return XPaging(paging?.count)
        }
    }
}
