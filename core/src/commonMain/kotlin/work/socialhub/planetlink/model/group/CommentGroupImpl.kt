package work.socialhub.planetlink.model.group

import kotlinx.datetime.Instant
import net.socialhub.planetlink.model.group.CommentsRequestGroup
import work.socialhub.planetlink.action.group.CommentGroupAction
import work.socialhub.planetlink.action.group.CommentGroupActionImpl
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Instance
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging

/**
 * Whole Accounts Comments
 */
class CommentGroupImpl(
    /** Comments Request Group  */
    override var requestGroup: CommentsRequestGroup,
) : CommentGroup {

    /** Entity of Comments related to Request  */
    override var entities = mutableMapOf<CommentsRequest, Pageable<Comment>>()

    /** Max Date (include)  */
    override var maxDate: Instant? = null

    /** Since Date (not include)  */
    override var sinceDate: Instant? = null

    /**
     * {@inheritDoc}
     */
    override fun comments(): Pageable<Comment> {
        var stream = entities!!.values.flatMap { it.entities!! }

        val size = entities!!.values
            .filter { it.paging != null }
            .map { it.paging!!.count }
            .minBy { it!! }

        if (maxDate != null) {
            stream = stream.filter {
                it.createAt!!.toEpochMilliseconds() <= maxDate!!.toEpochMilliseconds()
            }
        }
        if (sinceDate != null) {
            stream = stream.filter {
                it.createAt!!.toEpochMilliseconds() > sinceDate!!.toEpochMilliseconds()
            }
        }

        stream = stream
            .sortedBy { it.createAt!!.toEpochMilliseconds() }
            .reversed()

        val result = Pageable<Comment>()

        result.setEntities(stream)
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

        // 各リクエストの中で最も SinceDate が過去ものを取得
        sinceDate = entities!!.values
            .filter { it.entities != null }
            .filter { it.entities!!.isNotEmpty() } //
            .map { it.entities!![it.entities!!.size - 1] } //
            .map { it.createAt }
            .maxBy { it!!.toEpochMilliseconds() }

        // -1ms (for not include)
        if (sinceDate != null) {
            val mSec = sinceDate.toEpochMilliseconds() - 1
            sinceDate = Instant.fromEpochMilliseconds(mSec)
        }
    }

    override fun action(): CommentGroupAction {
        return CommentGroupActionImpl(this)
    }

    fun setNewestComment(comment: Comment) {
        maxDate = comment.createAt

        // リクエストが単数の場合
        // -> ページネーションにも通知
        if (entities!!.size == 1) {
            val entity = entities!!.values.iterator().next()
            entity.setNewestIdentify(comment)
        }
    }

    fun setOldestComment(comment: Comment) {
        sinceDate = comment.createAt

        // リクエストが単数の場合
        // -> ページネーションにも通知
        if (entities!!.size == 1) {
            val entity = entities!!.values.iterator().next()
            entity.setOldestIdentify(comment)
        }
    }
}
