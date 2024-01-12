package work.socialhub.planetlink.model.group

import kotlinx.datetime.Instant
import work.socialhub.planetlink.action.group.CommentGroupAction
import work.socialhub.planetlink.action.group.CommentGroupActionImpl
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Comment
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
        var stream = entities.values.flatMap { it.entities }

        val size = entities.values
            .filter { it.paging != null }
            .map { it.paging!!.count!! }
            .minBy { it }

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

        return Pageable<Comment>().also {
            it.paging = Paging(size)
            it.entities = stream
        }
    }

    /**
     * Marge Prev Comments when New Request
     * 最新リクエストの場合の結合処理
     */
    fun margeWhenNewPageRequest(prev: CommentGroupImpl) {
        if (prev.maxDate == null) {
            return
        }

        // MaxDate を変更
        sinceDate = prev.maxDate

        entities.forEach { (acc, page) ->
            if (prev.entities.containsKey(acc)) {
                val comments = mutableListOf<Comment>()
                comments.addAll(page.entities)

                comments.addAll(
                    prev.entities[acc]!!.entities
                        .filter { it.createAt!!.toEpochMilliseconds() > sinceDate!!.toEpochMilliseconds() }
                )

                page.entities = comments
                    .sortedBy { it.createAt!!.toEpochMilliseconds() }
                    .reversed()
            }
        }
    }

    /**
     * Marge Prev Comments when Past Request
     * 遡りリクエストの場合の結合処理
     */
    fun margeWhenPastPageRequest(prev: CommentGroupImpl) {
        if (prev.sinceDate == null) {
            return
        }

        // MaxDate を変更
        maxDate = prev.sinceDate

        entities.forEach { (acc, page) ->
            if (prev.entities.containsKey(acc)) {
                val comments = mutableListOf<Comment>()
                comments.addAll(page.entities)

                comments.addAll(
                    prev.entities[acc]!!.entities
                        .filter { it.createAt!!.toEpochMilliseconds() <= maxDate!!.toEpochMilliseconds() }
                )

                page.entities = comments
                    .sortedBy { it.createAt!!.toEpochMilliseconds() }
                    .reversed()
            }
        }
    }

    /**
     * MaxDate の計算
     */
    fun setMaxDateFromEntities() {
        maxDate = entities.values
            .filter { it.entities.isNotEmpty() }
            .map { it.entities[0] }
            .map { it.createAt }

            // 各リクエストの中で最も MaxDate が最新ものを取得
            .minBy { it!!.toEpochMilliseconds() }
    }

    /**
     * SinceDate の計算
     */
    fun setSinceDateFromEntities() {

        // 各リクエストの中で最も SinceDate が過去ものを取得
        sinceDate = entities.values
            .filter { it.entities.isNotEmpty() } //
            .map { it.entities[it.entities.size - 1] } //
            .map { it.createAt }
            .maxBy { it!!.toEpochMilliseconds() }

        // -1ms (for not include)
        if (sinceDate != null) {
            val mSec = sinceDate!!.toEpochMilliseconds() - 1
            sinceDate = Instant.fromEpochMilliseconds(mSec)
        }
    }

    override fun action(): CommentGroupAction {
        return CommentGroupActionImpl(this)
    }

    override fun setNewestComment(comment: Comment) {
        maxDate = comment.createAt

        // リクエストが単数の場合
        // -> ページネーションにも通知
        if (entities.size == 1) {
            val entity = entities.values.iterator().next()
            entity.setNewestIdentify(comment)
        }
    }

    override fun setOldestComment(comment: Comment) {
        sinceDate = comment.createAt

        // リクエストが単数の場合
        // -> ページネーションにも通知
        if (entities.size == 1) {
            val entity = entities.values.iterator().next()
            entity.setOldestIdentify(comment)
        }
    }
}
