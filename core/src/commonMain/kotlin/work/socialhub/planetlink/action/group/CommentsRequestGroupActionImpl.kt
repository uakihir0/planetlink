package work.socialhub.planetlink.action.group

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Pageable
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
        val entities = mutableMapOf<CommentsRequest, Pageable<Comment>>()

        coroutineScope {
            requestGroup.requests.map {
                async { entities[it] = it.comments(paging) }
            }.awaitAll()
        }

        return CommentGroupImpl(requestGroup).also {
            it.entities.putAll(entities)
            it.maxDate = Clock.System.now()
            it.setSinceDateFromEntities()
        }
    }
}
