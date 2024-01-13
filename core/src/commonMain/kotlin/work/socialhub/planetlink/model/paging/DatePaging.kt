package work.socialhub.planetlink.model.paging

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Paging with date range
 * 時間範囲指定
 * (Slack)
 */
class DatePaging : Paging() {

    var latest: String? = null

    var oldest: String? = null

    var inclusive: Boolean? = null

    /**
     * {@inheritDoc}
     */
    override fun <K : Identify> newPage(entities: List<K>): Paging {
        val newPage = DatePaging()
        newPage.count = count

        if (entities.isNotEmpty()) {
            val first = entities[0].id as String?
            newPage.inclusive = false
            newPage.oldest = first
            return newPage

        } else {
            // デフォルト動作
            if (latest != null && inclusive != null) {
                newPage.inclusive = !inclusive!!
                newPage.oldest = latest
                return newPage
            }

            // 上記以外は再度リクエスト
            return this.copy()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun <K : Identify> pastPage(entities: List<K>): Paging {
        val newPage = DatePaging()
        newPage.count = count

        if (entities.isNotEmpty()) {
            val index = (entities.size - 1)
            val last = entities[index].id as String?

            newPage.inclusive = false
            newPage.latest = last
            return newPage
        } else {

            // デフォルト動作
            if (oldest != null && inclusive != null) {
                newPage.inclusive = !inclusive!!
                newPage.latest = oldest
                return newPage
            }

            // 上記以外は再度リクエスト
            return this.copy()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun setMarkPagingEnd(entities: List<*>) {
        if (isHasPast
            && entities.isEmpty()
            && (oldest == null)
            && (count!! > 0)
        ) {
            isHasPast = false
        }
    }

    /**
     * オプジェクトコピー
     */
    override fun copy(): DatePaging {
        return DatePaging().also {
            it.latest = latest
            it.oldest = oldest
            it.inclusive = inclusive
            copyTo(it)
        }
    }

    companion object {

        /**
         * From Paging instance
         */
        fun fromPaging(paging: Paging?): DatePaging {
            if (paging is DatePaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = DatePaging()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
