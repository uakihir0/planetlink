package work.socialhub.planetlink.model.paging

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Paging with page number
 * ベージ番号付きページング
 */
class IndexPaging : Paging() {

    var page: Long? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(entities: List<T>): Paging {
        if (page!! > 1) {
            val newPage = IndexPaging()
            newPage.count = count
            newPage.page = page!! - 1
            return newPage
        }

        return copy()
    }

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> pastPage(entities: List<T>): Paging {
        val number = ((if ((page == null)) 0L else page)!! + 1L)
        val pastPage = IndexPaging()
        pastPage.count = count
        pastPage.page = number
        return pastPage
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): IndexPaging {
        val pg = IndexPaging()
        pg.page = page
        copyTo(pg)
        return pg
    }

    companion object {

        /**
         * From Paging instance
         */
        fun fromPaging(paging: Paging?): IndexPaging {
            if (paging is IndexPaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = IndexPaging()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
