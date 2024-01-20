package work.socialhub.planetlink.bluesky.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

class BlueskyPaging(
    count: Int? = null,
) : Paging(count) {

    /**
     * 最新のレコードの記録
     */
    var latestRecord: Identify? = null
    var latestRecordHint: Identify? = null

    /**
     * ページングカーソル
     */
    var cursor: String? = null
    var cursorHint: String? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {

        val pg = copy()
        if (entities.isNotEmpty()) {

            // ヒントが設定されている場合はそれを使用
            if (latestRecordHint != null) {
                pg.latestRecord = latestRecordHint
                pg.cursor = null
                return pg.clearHint()
            }

            val first = entities[0]
            pg.latestRecord = first
            pg.cursor = null
            return pg.clearHint()
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

            // ヒントが設定されている場合はそれを使用
            if (cursorHint != null) {
                pg.cursor = cursorHint
                pg.latestRecord = null
                return pg.clearHint()
            }

            val count = entities.size
            val last = entities[count - 1]

            if (last is BlueskyComment) {
                val cursor = "${last.createAt!!.toEpochMilliseconds()}::${last.cid}"
                pg.cursor = cursor
                pg.latestRecord = null
                return pg.clearHint()
            }
        }

        return pg
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): BlueskyPaging {
        return BlueskyPaging().also {
            it.latestRecord = latestRecord
            it.latestRecordHint = latestRecordHint
            it.cursor = cursor
            it.cursorHint = cursorHint
            copyTo(it)
        }
    }

    private fun clearHint(): BlueskyPaging {
        latestRecordHint = null
        cursorHint = null
        return this
    }

    companion object {
        /**
         * From Paging instance
         */
        fun fromPaging(
            paging: Paging?,
        ): BlueskyPaging {

            if (paging is BlueskyPaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = BlueskyPaging()
            if (paging?.count != null) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
