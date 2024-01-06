package net.socialhub.planetlink.model.paging

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Paging.copyTo
import work.socialhub.planetlink.model.Paging.count
import work.socialhub.planetlink.model.Paging.isHasPast

/**
 * Paging with date range
 * 時間範囲指定
 * (Slack)
 */
class DatePaging : Paging() {
    //region // Getter&Setter
    var latest: String? = null

    var oldest: String? = null

    //endregion
    var inclusive: Boolean? = null

    /**
     * {@inheritDoc}
     */
    override fun <K : Identify?> newPage(entities: List<K>?): Paging {
        val newPage = DatePaging()
        newPage.count = count

        if (entities != null && !entities.isEmpty()) {
            val first = entities[0]!!.id as String?

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
    override fun <K : Identify?> pastPage(entities: List<K>?): Paging {
        val newPage = DatePaging()
        newPage.count = count

        if (entities != null && !entities.isEmpty()) {
            val index = (entities.size - 1)
            val last = entities[index]!!.id as String?

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
            && (count > 0)
        ) {
            isHasPast = false
        }
    }

    /**
     * オプジェクトコピー
     */
    override fun copy(): DatePaging {
        val pg = DatePaging()
        pg.latest = latest
        pg.oldest = oldest
        pg.inclusive = inclusive
        copyTo(pg)
        return pg
    }

    companion object {
        /**
         * From Paging instance
         */
        fun fromPaging(paging: Paging?): DatePaging {
            if (paging is DatePaging) {
                return (paging as DatePaging).copy()
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
