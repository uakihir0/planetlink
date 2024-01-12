package work.socialhub.planetlink.model.paging

import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * ID の開始と終了を指定するページング
 * (start,end の片方のみを指定する)
 */
class RangePaging(
    count: Int? = null,
) : Paging(count) {

    /** 開始 ID  */
    var start: ID? = null
    var startHint: ID? = null

    /** 終了 ID  */
    var end: ID? = null
    var endHint: ID? = null

    /**
     * {@inheritDoc}
     */
    override fun <J : Identify> newPage(
        entities: List<J>
    ): Paging {
        val pg = copy()

        if (entities.isNotEmpty()) {
            if (endHint != null) {
                pg.end = endHint
                pg.start = null
                return pg.clearHint()
            }

            pg.end = entities[0].id
            pg.start = null
            return pg.clearHint()
        }

        return pg
    }

    /**
     * {@inheritDoc}
     */
    override fun <J : Identify> pastPage(
        entities: List<J>
    ): Paging {
        val pg = copy()

        if (entities.isNotEmpty()) {
            if (startHint != null) {
                pg.start = startHint
                pg.end = null
                return pg.clearHint()
            }

            val len = entities.size
            pg.start = entities[len - 1].id
            pg.end = null
            return pg.clearHint()
        }

        return pg
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): RangePaging {
        return RangePaging().also {
            it.start = start
            it.startHint = startHint
            it.end = end
            it.endHint = endHint
            copyTo(it)
        }
    }

    private fun clearHint(): RangePaging {
        startHint = null
        endHint = null
        return this
    }


    companion object {
        /**
         * From Paging instance
         */
        fun <T> fromPaging(paging: Paging?): RangePaging {
            if (paging is RangePaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = RangePaging()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
