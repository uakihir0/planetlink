package net.socialhub.planetlink.model

/**
 * ページング可能レスポンス
 * Pageable Response
 */
class Pageable<T : Identify?> : java.io.Serializable {
    /** Paging Information  */
    private var paging: Paging? = null

    /** Entities  */
    private var entities: List<T>? = null

    /** Displayable predicate  */
    private var predicate: java.util.function.Predicate<T>? = null

    /**
     * Get New Page
     * 最新のページを取得
     */
    fun newPage(): Paging? {
        return paging!!.newPage(entities)
    }

    /**
     * Get Past Page
     * 過去のページを取得
     */
    fun pastPage(): Paging? {
        return paging!!.pastPage(entities)
    }

    /**
     * Get Prev Page
     * 前のページを取得
     */
    fun prevPage(): Paging? {
        return paging!!.prevPage(entities)
    }

    /**
     * Get Next Page
     * 次のページを取得
     */
    fun nextPage(): Paging? {
        return paging!!.nextPage(entities)
    }

    /**
     * Set newest boarder identify. (for streaming)
     */
    fun setNewestIdentify(identify: T) {
        val model: List<T> = java.util.ArrayList<T>(entities)
        model.add(0, identify)
        setEntities(model)
    }

    /**
     * Set oldest boarder identify.
     */
    fun setOldestIdentify(identify: T) {
        val model: MutableList<T> = java.util.ArrayList<T>(entities)
        model.add(identify)
        setEntities(model)
    }

    val displayableEntities: List<T>?
        /**
         * Get Displayable Entities
         * 表示条件を満たしたアイテムを取得
         */
        get() {
            if (predicate == null) {
                return getEntities()
            }
            return getEntities() //
                .stream() //
                .filter(predicate) //
                .collect<List<T>, Any>(java.util.stream.Collectors.toList<T>())
        }

    // region // Getter&Setter
    fun getPaging(): Paging? {
        return paging
    }

    fun setPaging(paging: Paging?) {
        this.paging = paging
        if (this.paging != null && this.entities != null) {
            this.paging.setMarkPagingEnd(this.entities)
        }
    }

    fun getEntities(): List<T>? {
        return entities
    }

    fun setEntities(entities: List<T>?) {
        this.entities = entities
        if (this.paging != null && this.entities != null) {
            paging.setMarkPagingEnd(this.entities)
        }
    }

    fun getPredicate(): java.util.function.Predicate<T>? {
        return predicate
    }

    fun setPredicate(predicate: java.util.function.Predicate<T>) {
        this.predicate = predicate
    } // endregion
}
