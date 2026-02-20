package work.socialhub.planetlink.action.group

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.group.CommentGroup
import work.socialhub.planetlink.model.group.CommentGroupImpl

/**
 * Actions for Comment Group
 * コメントグループに対しての操作
 */
class CommentGroupActionImpl(
    private val commentGroup: CommentGroupImpl
) : CommentGroupAction {

    /**
     * {@inheritDoc}
     */
    override suspend fun newComments(): CommentGroup {
        val entities = mutableMapOf<CommentsRequest, Pageable<Comment>>()

        coroutineScope {
            commentGroup.entities.entries.map { (k, v) ->
                async { entities[k] = k.comments(v.newPage()) }
            }.awaitAll()
        }

        return CommentGroupImpl(
            commentGroup.requestGroup
        ).also {
            it.entities.putAll(entities)
            it.setMaxDateFromEntities()
            it.margeWhenNewPageRequest(commentGroup)
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun pastComments(): CommentGroup {
        val entities = mutableMapOf<CommentsRequest, Pageable<Comment>>()

        coroutineScope {
            commentGroup.entities.entries.map { (k, v) ->
                async { entities[k] = k.comments(v.pastPage()) }
            }.awaitAll()
        }

        return CommentGroupImpl(
            commentGroup.requestGroup
        ).also {
            it.entities.putAll(entities)
            it.setSinceDateFromEntities()
            it.margeWhenPastPageRequest(commentGroup)
        }
    }
}
