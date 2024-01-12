package work.socialhub.planetlink.model.paging

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Paging with cursor
 * カーソル付きページング
 * (Twitter, Slack etc.)
 */
class CursorPaging<Type>(
    count: Int? = null,
) : Paging(count) {

    /** prev cursor  */
    var prevCursor: Type? = null

    /** current cursor  */
    var currentCursor: Type? = null

    /** next cursor  */
    var nextCursor: Type? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        val newPage = CursorPaging<Type>()

        if (prevCursor != null) {
            newPage.currentCursor = prevCursor
            newPage.count = count
            return newPage
        }

        return newPage
    }

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        val pastPage = CursorPaging<Type>()

        if (nextCursor != null) {
            pastPage.currentCursor = nextCursor
            pastPage.count = count
            return pastPage
        }

        return pastPage
    }

    /**
     * {@inheritDoc}
     */
    override fun setMarkPagingEnd(
        entities: List<*>
    ) {
        if (isHasNew && (prevCursor == null)) {
            isHasNew = false
        }
        if (isHasPast && (nextCursor == null)) {
            isHasPast = false
        }
    }

    /**
     * オプジェクトコピー
     */
    override fun copy(): CursorPaging<Type> {
        return CursorPaging<Type>().also {
            it.currentCursor = currentCursor
            it.nextCursor = nextCursor
            it.prevCursor = prevCursor
            copyTo(it)
        }
    }

    companion object {
        /**
         * From Paging instance
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> fromPaging(paging: Paging?): CursorPaging<T> {
            if (paging is CursorPaging<*>) {
                return (paging as CursorPaging<T>).copy()
            }

            // Count の取得
            val pg = CursorPaging<T>()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
