package work.socialhub.planetlink.tumblr.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Tumblr Paging
 * Tumblr の特殊ページング対応
 */
class TumblrPaging : Paging() {

    var sinceId: String? = null
    var offset: Int? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        return copy().also { pg ->
            if (entities.isNotEmpty()) {
                val first = entities[0]
                pg.sinceId = first.id<String>()
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        return copy().also { pg ->
            if (entities.isNotEmpty()) {
                val count = entities.size.toLong()
                if (pg.offset == null) {
                    pg.offset = 0
                }

                // オフセット分を取得した量分変更
                pg.offset = pg.offset!! + count.toInt()
            }
        }
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): TumblrPaging {
        return TumblrPaging().also { pg ->
            pg.offset = offset
            pg.sinceId = sinceId
            copyTo(pg)
        }
    }

    companion object {
        /**
         * From Paging instance
         */
        fun fromPaging(
            paging: Paging?
        ): TumblrPaging {
            if (paging is TumblrPaging) {
                return paging.copy()
            }

            // Count の取得
            return TumblrPaging().also { pg ->
                if ((paging != null) && (paging.count != null)) {
                    pg.count = paging.count
                }
            }
        }
    }
}
