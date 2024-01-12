package work.socialhub.planetlink.model.paging

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Paging with max and since
 * Max Since 管理のページング
 * (Twitter, Mastodon etc.)
 */
class BorderPaging(
    count: Int? = null,
) : Paging(count) {

    /** Max Id  */
    var maxId: Long? = null
    var maxInclude: Boolean = true

    /** Since Id  */
    var sinceId: Long? = null
    var sinceInclude: Boolean = false

    /** Hint to next paging  */
    var hintNewer: Boolean = false

    /**
     * ID のスキップ単位
     * Mastodon のタイムラインに於いて MaxID を指定した場合
     * (MaxID + 1) の ID のコメントが取得出来ないので追加
     * TODO: Mastodon の実装を眺めて実装を確認する
     */
    var idUnit: Long = 1L

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        val newPage = BorderPaging()
        newPage.sinceInclude = sinceInclude
        newPage.maxInclude = maxInclude
        newPage.idUnit = idUnit
        newPage.count = count
        newPage.hintNewer = true

        if (entities.isNotEmpty()) {
            val offset = if (sinceInclude) 1L else 0L
            val id = parseNumber(entities[0].id)
            newPage.sinceId = id + (offset * idUnit)
            return newPage

            // [offset]
            // m  s
            // in in +1
            // ex in +1
            // in ex 0
            // ex ex 0
        } else {
            if (maxId != null) {
                val offset = (-1 + (if (sinceInclude) 1L else 0L) + (if (maxInclude) 1L else 0L))
                newPage.sinceId = maxId!! + (offset * idUnit)
                return newPage

                // [offset]
                // m  s
                // in in +1
                // ex in 0
                // in ex 0
                // ex ex -1
            }
            return this.copy()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        val newPage = BorderPaging()
        newPage.sinceInclude = sinceInclude
        newPage.maxInclude = maxInclude
        newPage.idUnit = idUnit
        newPage.count = count

        if (entities.isNotEmpty()) {
            val offset = if (maxInclude) -1L else 0L
            val last = entities[entities.size - 1]
            val id = parseNumber(last.id)
            newPage.maxId = id + (offset * idUnit)
            return newPage

            // [offset]
            // m  s
            // in in -1
            // in ex -1
            // ex in 0
            // ex ex 0
        } else {
            if (sinceId != null) {
                val offset = (1 + (if (maxInclude) -1L else 0L) + (if (sinceInclude) -1L else 0L))
                newPage.maxId = sinceId!! + (offset * idUnit)
                return newPage

                // [offset]
                // m  s
                // in in -1
                // in ex 0
                // ex in 0
                // ex ex +1
            }
            return this.copy()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun setMarkPagingEnd(
        entities: List<*>
    ) {
        if (isHasPast
            && entities.isEmpty()
            && (sinceId == null)
            && (count!! > 0)
        ) {
            isHasPast = false
        }
    }

    private fun parseNumber(id: Any?): Long {
        try {
            if (id is Long) {
                return id
            }
            if (id is String) {
                return id.toLong()
            }
            throw IllegalStateException("invalid format id: $id")
        } catch (e: Exception) {
            throw IllegalStateException("invalid format id: $id", e)
        }
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): BorderPaging {
        return BorderPaging().also {
            it.maxId = maxId
            it.sinceId = sinceId
            it.maxInclude = maxInclude
            it.sinceInclude = sinceInclude
            it.hintNewer = hintNewer
            it.idUnit = idUnit
            copyTo(it)
        }
    }

    companion object {
        /**
         * From Paging instance
         */
        fun fromPaging(paging: Paging?): BorderPaging {
            if (paging is BorderPaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = BorderPaging()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }

            return pg
        }
    }
}
