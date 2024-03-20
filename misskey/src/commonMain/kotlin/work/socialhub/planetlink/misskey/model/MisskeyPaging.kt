package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Misskey Paging
 * Misskey の特殊ページングに対応
 */
class MisskeyPaging : Paging() {

    var untilId: String? = null
    var sinceId: String? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        val pg = copy()

        if (entities.isNotEmpty()) {
            val first = entities.first()
            pg.untilId = null

            // Comment の場合はページング用 ID を使用
            if (first is MisskeyComment) {
                pg.sinceId = first.idForPaging

            } else {
                // 他のオブジェクトはそのままのを使用
                pg.sinceId = first.id<String>()
            }
        }
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
            val last = entities.last()
            pg.sinceId = null

            // Comment の場合はページング用 ID を使用
            if (last is MisskeyComment) {
                pg.untilId = last.pagingId

            } else {
                // 他のオブジェクトはそのままのを使用
                pg.untilId = last.id<String>()
            }
        }
        return pg
    }

    /**
     * オブジェクトコピー
     */
    override fun copy() =
        MisskeyPaging().also {
            it.sinceId = sinceId
            it.untilId = untilId
            copyTo(it)
        }

    companion object {

        /**
         * From Paging instance
         */
        fun fromPaging(
            paging: Paging?
        ): MisskeyPaging {

            if (paging is MisskeyPaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = MisskeyPaging()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
