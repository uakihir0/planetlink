package work.socialhub.planetlink.mastodon.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging

/**
 * Mastodon Paging
 * Mastodon の特殊ページングに対応
 */
class MastodonPaging : Paging() {

    var minId: String? = null
    var minIdInLink: String? = null
    var maxId: String? = null
    var maxIdInLink: String? = null

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> newPage(
        entities: List<T>
    ): Paging {
        val pg = copy()
        if (entities.isEmpty()) {
            return pg
        }

        return pg.also {
            it.maxId = null
            it.minId = minIdInLink
                ?: entities[0].id<String>()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun <T : Identify> pastPage(
        entities: List<T>
    ): Paging {
        val pg = copy()
        if (entities.isEmpty()) {
            return pg
        }

        return pg.also {
            it.minId = null
            it.maxId = maxIdInLink
                ?: entities[entities.lastIndex].id<String>()
        }
    }

    /**
     * オブジェクトコピー
     */
    override fun copy(): MastodonPaging {
        return MastodonPaging().also {
            it.maxId = maxId
            it.minId = minId
            copyTo(it)
        }
    }

    companion object {

        /**
         * From Paging instance
         */
        fun fromPaging(
            paging: Paging?
        ): MastodonPaging {

            if (paging is MastodonPaging) {
                return paging.copy()
            }

            // Count の取得
            val pg = MastodonPaging()
            if ((paging != null) && (paging.count != null)) {
                pg.count = paging.count
            }
            return pg
        }
    }
}
