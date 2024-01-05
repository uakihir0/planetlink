package net.socialhub.planetlink.model.group

import net.socialhub.planetlink.action.group.CommentGroupAction
import net.socialhub.planetlink.model.Instance
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Comment

/**
 * Whole Accounts Comments
 */
class CommentGroupImpl : CommentGroup {
    //region // Getter&Setter
    /** Comments Request Group  */
    override var requestGroup: CommentsRequestGroup? = null

    /** Entity of Comments related to Request  */
    override var entities: Map<CommentsRequest, Pageable<Comment>>? = null

    /** Max Date (include)  */
    override var maxDate: java.util.Date? = null

    /** Since Date (not include)  */
    override var sinceDate: java.util.Date? = null

    override val comments: Pageable<Comment?>?
        /**
         * {@inheritDoc}
         */
        get() {
            var stream: java.util.stream.Stream<Comment?> = entities!!.values.stream() //
                .flatMap<Comment>(java.util.function.Function<Pageable<Comment>, java.util.stream.Stream<out Comment>> { e: Pageable<Comment?> ->
                    e.getEntities().stream()
                })

            val size: Long = entities!!.values.stream()
                .filter(java.util.function.Predicate<Pageable<Comment>> { e: Pageable<Comment?> -> (e.getPaging() != null) })
                .map<Long>(java.util.function.Function<Pageable<Comment>, Long> { e: Pageable<Comment?> -> e.getPaging()!!.count })
                .min(java.util.Comparator<Long> { obj: Long, anotherLong: Long? ->
                    obj.compareTo(
                        anotherLong!!
                    )
                }).orElse(0L)

            if (maxDate != null) {
                stream =
                    stream.filter(java.util.function.Predicate<Comment> { e: Comment -> e.createAt.getTime() <= maxDate.getTime() })
            }
            if (sinceDate != null) {
                stream =
                    stream.filter(java.util.function.Predicate<Comment> { e: Comment -> e.createAt.getTime() > sinceDate.getTime() })
            }

            stream = stream.sorted(java.util.Comparator.comparing(Comment::createAt).reversed())
            val comments: List<Comment> =
                stream.collect<List<Comment>, Any>(java.util.stream.Collectors.toList<Comment>())
            val result = Pageable<Comment>()

            result.setEntities(comments)
            result.setPaging(Paging(size))
            return result
        }

    /**
     * Marge Prev Comments when New Request
     * 最新リクエストの場合の結合処理
     */
    fun margeWhenNewPageRequest(prev: CommentGroupImpl) {
        if (prev.getMaxDate() == null) {
            return
        }

        // MaxDate を変更
        sinceDate = prev.getMaxDate()

        entities.forEach(BiConsumer<CommentsRequest, Pageable<Comment>> { acc: CommentsRequest, page: Pageable<Comment?> ->
            if (prev.getEntities()!!
                    .containsKey(acc)
            ) {
                val comments: MutableList<Comment?> = java.util.ArrayList<Comment>(page.getEntities())
                comments.addAll(
                    prev.getEntities()!![acc]!!.getEntities().stream() //
                        .filter(java.util.function.Predicate<Comment> { e: Comment -> e.createAt.getTime() > sinceDate.getTime() }) //
                        .collect<List<Comment>, Any>(java.util.stream.Collectors.toList<Comment>())
                )

                comments.sort(java.util.Comparator.comparing(Comment::createAt).reversed())
                page.setEntities(comments)
            }
        })
    }

    /**
     * Marge Prev Comments when Past Request
     * 遡りリクエストの場合の結合処理
     */
    fun margeWhenPastPageRequest(prev: CommentGroupImpl) {
        if (prev.getSinceDate() == null) {
            return
        }

        // MaxDate を変更
        maxDate = prev.getSinceDate()

        entities.forEach(BiConsumer<CommentsRequest, Pageable<Comment>> { acc: CommentsRequest, page: Pageable<Comment?> ->
            if (prev.getEntities()!!
                    .containsKey(acc)
            ) {
                val comments: MutableList<Comment?> = java.util.ArrayList<Comment>(page.getEntities())
                comments.addAll(
                    prev.getEntities()!![acc]!!.getEntities().stream() //
                        .filter(java.util.function.Predicate<Comment> { e: Comment -> e.createAt.getTime() <= maxDate.getTime() }) //
                        .collect<List<Comment>, Any>(java.util.stream.Collectors.toList<Comment>())
                )

                comments.sort(java.util.Comparator.comparing(Comment::createAt).reversed())
                page.setEntities(comments)
            }
        })
    }

    /**
     * MaxDate の計算
     */
    fun setMaxDateFromEntities() {
        maxDate = entities!!.values.stream() //
            .filter(java.util.function.Predicate<Pageable<Comment>> { e: Pageable<Comment?> -> e.getEntities() != null }) //
            .filter(java.util.function.Predicate<Pageable<Comment>> { e: Pageable<Comment?> ->
                !e.getEntities()!!
                    .isEmpty()
            }) //
            .map<Comment>(java.util.function.Function<Pageable<Comment>, Comment> { e: Pageable<Comment?> -> e.getEntities()!![0] }) //
            .map<Instance>(Comment::createAt) //
            // 各リクエストの中で最も MaxDate が最新ものを取得

            .min(java.util.Comparator<Instance> { obj: java.util.Date, anotherDate: java.util.Date? ->
                obj.compareTo(
                    anotherDate
                )
            }) //
            .orElse(null) //
    }

    /**
     * SinceDate の計算
     */
    fun setSinceDateFromEntities() {
        sinceDate = entities!!.values.stream() //
            .filter(java.util.function.Predicate<Pageable<Comment>> { e: Pageable<Comment?> -> e.getEntities() != null }) //
            .filter(java.util.function.Predicate<Pageable<Comment>> { e: Pageable<Comment?> ->
                !e.getEntities()!!
                    .isEmpty()
            }) //
            .map<Comment>(java.util.function.Function<Pageable<Comment>, Comment> { e: Pageable<Comment?> -> e.getEntities()!![e.getEntities()!!.size - 1] }) //
            .map<Instance>(Comment::createAt) //
            // 各リクエストの中で最も SinceDate が過去ものを取得

            .max(java.util.Comparator<Instance> { obj: java.util.Date, anotherDate: java.util.Date? ->
                obj.compareTo(
                    anotherDate
                )
            }) //
            .orElse(null) //

        // -1ms (for not include)
        if (sinceDate != null) {
            sinceDate = java.util.Date(sinceDate.getTime() - 1)
        }
    }

    override fun action(): CommentGroupAction {
        return CommentGroupActionImpl(this)
    }

    fun setNewestComment(comment: Comment) {
        setMaxDate(comment.createAt)

        // リクエストが単数の場合
        // -> ページネーションにも通知
        if (entities!!.size == 1) {
            val entity = entities!!.values.iterator().next()
            entity.setNewestIdentify(comment)
        }
    }

    fun setOldestComment(comment: Comment) {
        setSinceDate(comment.createAt)

        // リクエストが単数の場合
        // -> ページネーションにも通知
        if (entities!!.size == 1) {
            val entity = entities!!.values.iterator().next()
            entity.setOldestIdentify(comment)
        }
    }

    fun getEntities(): Map<CommentsRequest, Pageable<Comment>>? {
        return entities
    }

    fun setEntities(entities: Map<CommentsRequest, Pageable<Comment>>?) {
        this.entities = entities
    }

    fun getMaxDate(): java.util.Date? {
        return maxDate
    }

    fun setMaxDate(maxDate: java.util.Date?) {
        this.maxDate = maxDate
    }

    fun getSinceDate(): java.util.Date? {
        return sinceDate
    }

    fun setSinceDate(sinceDate: java.util.Date?) {
        this.sinceDate = sinceDate
    } //endregion
}
