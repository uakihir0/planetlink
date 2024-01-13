package work.socialhub.planetlink.model

/**
 * ページング可能レスポンス
 * Pageable Response
 */
class Pageable<T : Identify> {

    /** Paging Information  */
    var paging: Paging? = null
        set(value) {
            field = value
            setMarkPagingEnd()
        }

    /** Entities  */
    var entities: List<T> = listOf()
        set(value) {
            field = value
            setMarkPagingEnd()
        }

    /** Displayable predicate  */
    var predicate: ((T) -> Boolean)? = null

    /**
     * Get New Page
     * 最新のページを取得
     */
    fun newPage(): Paging {
        return paging!!.newPage(entities)
    }

    /**
     * Get Past Page
     * 過去のページを取得
     */
    fun pastPage(): Paging {
        return paging!!.pastPage(entities)
    }

    /**
     * Get Prev Page
     * 前のページを取得
     */
    fun prevPage(): Paging {
        return paging!!.prevPage(entities)
    }

    /**
     * Get Next Page
     * 次のページを取得
     */
    fun nextPage(): Paging {
        return paging!!.nextPage(entities)
    }

    /**
     * Set newest boarder identify. (for streaming)
     */
    fun setNewestIdentify(identify: T) {
        val model = mutableListOf<T>()
        model.addAll(entities)
        model.add(0, identify)
        entities = model
    }

    /**
     * Set oldest boarder identify.
     */
    fun setOldestIdentify(identify: T) {
        val model = mutableListOf<T>()
        model.addAll(entities)
        model.add(identify)
        entities = model
    }

    /**
     * Get Displayable Entities
     * 表示条件を満たしたアイテムを取得
     */
    val displayableEntities: List<T>
        get() {
            return predicate?.let { f ->
                entities.filter { f(it) }
            } ?: entities
        }


    /**
     * Set mark as paging end
     * ページの終端をマークする
     */
    fun setMarkPagingEnd() {
        paging?.setMarkPagingEnd(entities)
    }
}
