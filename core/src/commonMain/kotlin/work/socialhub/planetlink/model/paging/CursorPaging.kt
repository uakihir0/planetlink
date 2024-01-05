package net.socialhub.planetlink.model.paging

import net.socialhub.planetlink.model.Identify
import net.socialhub.planetlink.model.Paging
import net.socialhub.planetlink.model.Paging.copyTo
import net.socialhub.planetlink.model.Paging.count
import net.socialhub.planetlink.model.Paging.isHasNew
import net.socialhub.planetlink.model.Paging.isHasPast

/**
 * Paging with cursor
 * カーソル付きページング
 * (Twitter, Slack etc)
 */
class CursorPaging<Type> : Paging() {
    /** prev cursor  */
    private var prevCursor: Type? = null

    //region // Getter&Setter
    /** current cursor  */
    var currentCursor: Type? = null

    /** next cursor  */
    private var nextCursor: Type? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify?> newPage(entities: List<T>?): Paging {
        val newPage: CursorPaging<Type> = CursorPaging<Any>()

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
    override fun <T : Identify?> pastPage(entities: List<T>?): Paging {
        val pastPage: CursorPaging<Type> = CursorPaging<Any>()

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
    override fun setMarkPagingEnd(entities: List<*>?) {
        if (isHasNew && (getPrevCursor() == null)) {
            isHasNew = false
        }
        if (isHasPast && (getNextCursor() == null)) {
            isHasPast = false
        }
    }

    /**
     * オプジェクトコピー
     */
    override fun copy(): CursorPaging<Type?> {
        val pg: CursorPaging<Type?> = CursorPaging<Any?>()
        pg.currentCursor = currentCursor
        pg.setNextCursor(getNextCursor())
        pg.setPrevCursor(getPrevCursor())
        copyTo(pg)
        return pg
    }

    fun getPrevCursor(): Type? {
        return prevCursor
    }

    fun setPrevCursor(prevCursor: Type) {
        this.prevCursor = prevCursor
    }

    fun getNextCursor(): Type? {
        return nextCursor
    }

    fun setNextCursor(nextCursor: Type) {
        this.nextCursor = nextCursor
    } //endregion

    companion object {
        /**
         * From Paging instance
         */
        fun <T> fromPaging(paging: Paging?): CursorPaging<T> {
            if (paging is CursorPaging<*>) {
                return (paging as CursorPaging<T>).copy()
            }

            // Count の取得
            val pg: CursorPaging<T> = CursorPaging<Any>()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
