package work.socialhub.planetlink.model.paging

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

class OffsetPaging : Paging() {

    var offset: Long? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        val pg = copy()
        pg.offset = 0L
        return pg
    }

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        val pg = copy()

        if (entities.isNotEmpty()) {
            val count = entities.size.toLong()
            if (pg.offset == null) {
                pg.offset = 0L
            }

            // オフセット分を取得した量分変更
            pg.offset = pg.offset!! + count
        }
        return pg
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): OffsetPaging {
        val pg = OffsetPaging()
        pg.offset = offset
        copyTo(pg)
        return pg
    }

    companion object {
        /**
         * From Paging instance
         */
        fun fromPaging(paging: Paging?): OffsetPaging {
            if (paging is OffsetPaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = OffsetPaging()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}

