package work.socialhub.planetlink.model

import work.socialhub.planetlink.model.paging.*

/**
 * Paging
 * ページング情報
 * Specified Paging
 *
 * @see BorderPaging
 * @see CursorPaging
 * @see DatePaging
 * @see IndexPaging
 * @see OffsetPaging
 */
open class Paging(
    var count: Int? = null,
) {
    var isHasNew: Boolean = true
    var isHasPast: Boolean = true

    open fun copy() =
        Paging().also {
            it.count = count
            it.isHasNew = isHasNew
            it.isHasPast = isHasPast
        }

    fun copyTo(pg: Paging) {
        pg.count = count
        pg.isHasNew = isHasNew
        pg.isHasPast = isHasPast
    }

    /**
     * Get page for get newer entities
     * 新しい情報を取得するページを取得
     *
     * @param entities DataList it's ordered by created date time for desc.
     * 算出するデータリスト、先頭から最新の ID になっている想定
     */
    open fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        throw NotImplementedError()
    }

    /**
     * Get page for get past entities
     * 遡って過去の情報を取得するページを取得
     *
     * @param entities DataList it's ordered by created date time for desc.
     * 算出するデータリスト、先頭から最新の ID になっている想定
     */
    open fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        throw NotImplementedError()
    }

    /**
     * Alias
     * New <-> Prev
     */
    fun <T : Identify> prevPage(
        entities: List<T>
    ): Paging {
        return newPage(entities)
    }

    /**
     * Alias
     * Past <-> Next
     */
    fun <T : Identify> nextPage(
        entities: List<T>
    ): Paging {
        return pastPage(entities)
    }

    /**
     * Set mark as paging end
     * ページの終端をマークする
     */
    open fun setMarkPagingEnd(
        entities: List<*>
    ) {
        if (isHasPast
            && entities.isEmpty()
            && (count!! > 0)
        ) {
            isHasPast = false
        }
    }
}
