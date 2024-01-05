package net.socialhub.planetlink.model.paging

import net.socialhub.planetlink.model.Identify
import net.socialhub.planetlink.model.Paging
import net.socialhub.planetlink.model.Paging.copyTo
import net.socialhub.planetlink.model.Paging.count

/**
 * ID の開始と終了を指定するページング
 * (start,end の片方のみを指定する)
 */
class RangePaging<T> : Paging() {
    // region
    /** 開始 ID  */
    var start: T? = null
    var startHint: T? = null

    /** 終了 ID  */
    var end: T? = null

    // endregion
    var endHint: T? = null

    /**
     * {@inheritDoc}
     */
    fun <J : Identify?> newPage(entities: List<J>): Paging {
        val pg = copy()

        if (entities.size > 0) {
            if (endHint != null) {
                pg.end = endHint
                pg.start = null
                return pg.clearHint()
            }

            pg.end = entities[0]!!.id as T?
            pg.start = null
            return pg.clearHint()
        }

        return pg
    }

    /**
     * {@inheritDoc}
     */
    fun <J : Identify?> pastPage(entities: List<J>): Paging {
        val pg = copy()

        if (entities.size > 0) {
            if (startHint != null) {
                pg.start = startHint
                pg.end = null
                return pg.clearHint()
            }

            val len = entities.size
            pg.start = entities[len - 1]!!.id as T?
            pg.end = null
            return pg.clearHint()
        }

        return pg
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): RangePaging<T?> {
        val pg: RangePaging<T?> = RangePaging<Any?>()
        pg.start = start
        pg.startHint = startHint
        pg.end = end
        pg.endHint = endHint
        copyTo(pg)
        return pg
    }

    fun clearHint(): RangePaging<T> {
        startHint = null
        endHint = null
        return this
    }

    companion object {
        /**
         * From Paging instance
         */
        fun <T> fromPaging(paging: Paging?): RangePaging<T> {
            if (paging is RangePaging<*>) {
                return (paging as RangePaging<T>).copy()
            }

            // Count の取得
            val pg: RangePaging<T> = RangePaging<Any>()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
