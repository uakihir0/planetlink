package work.socialhub.planetlink.action.group

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.group.CommentGroup
import work.socialhub.planetlink.model.group.CommentGroupImpl
import work.socialhub.planetlink.model.group.CommentsRequestGroupImpl

class CommentsRequestGroupActionImpl(
    private val requestGroup: CommentsRequestGroupImpl
) : CommentsRequestGroupAction {

    /**
     * {@inheritDoc}
     */
    override suspend fun comments(
        count: Int
    ): CommentGroup {
        return comments(Paging(count))
    }

    /**
     * コメント情報をページング付きで取得
     */
    suspend fun comments(
        paging: Paging
    ): CommentGroup {
        // Collect the results first and add them in request order.
        // Writing to one map from several async blocks at once could crash,
        // and the order depended on which request finished first.
        val results = coroutineScope {
            requestGroup.requests.map {
                async { it to it.comments(paging) }
            }.awaitAll()
        }

        return CommentGroupImpl(requestGroup).also {
            it.entities.putAll(results)
            it.maxDate = Clock.System.now()
            it.setSinceDateFromEntities()
        }
    }
}
