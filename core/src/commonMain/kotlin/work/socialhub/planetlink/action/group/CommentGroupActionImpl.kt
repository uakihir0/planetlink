package work.socialhub.planetlink.action.group

import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.group.CommentGroup
import work.socialhub.planetlink.model.group.CommentGroupImpl
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Pageable

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
    override fun newComments(): CommentGroup {
        val model: CommentGroupImpl = CommentGroupImpl()
        val pool: ExecutorService = Executors.newCachedThreadPool()

        val futures: Map<CommentsRequest, java.util.concurrent.Future<Pageable<Comment>>> =
            commentGroup.getEntities().entries.stream()
                .collect<Map<CommentsRequest, java.util.concurrent.Future<Pageable<Comment>>>, Any>(
                    java.util.stream.Collectors.toMap<Any, Any, Any>(
                        java.util.function.Function<Any, Any> { java.util.Map.Entry.key },
                        java.util.function.Function<Any, Any> { entry: Map.Entry<error.NonExistentClass, Pageable<Comment?>> ->
                            pool.submit(
                                java.util.concurrent.Callable<T> {
                                    val paging: Paging? = entry.value.newPage()
                                    entry.key.getComments(paging)
                                })
                        })
                )

        val entities: Map<CommentsRequest, Pageable<Comment>> = futures
            .entries.stream().collect<Map<CommentsRequest, Pageable<Comment>>, Any>(
                java.util.stream.Collectors.toMap<Any, Any, Any>(
                    java.util.function.Function<Any, Any> { java.util.Map.Entry.key },
                    java.util.function.Function<Any, Any> { entry: Map.Entry<CommentsRequest?, java.util.concurrent.Future<Pageable<Comment?>?>> ->
                        HandlingUtil.runtime(
                            kotlin.jvm.functions.Function0<out T?> { entry.value.get() })
                    })
            )

        model.setEntities(entities)
        model.setMaxDateFromEntities()
        model.margeWhenNewPageRequest(commentGroup)
        model.requestGroup = commentGroup.requestGroup
        return model
    }

    /**
     * {@inheritDoc}
     */
    override fun pastComments(): CommentGroup {
        val model: CommentGroupImpl = CommentGroupImpl()
        val pool: ExecutorService = Executors.newCachedThreadPool()

        val futures: Map<CommentsRequest, java.util.concurrent.Future<Pageable<Comment>>> =
            commentGroup.getEntities().entries.stream()
                .collect<Map<CommentsRequest, java.util.concurrent.Future<Pageable<Comment>>>, Any>(
                    java.util.stream.Collectors.toMap<Any, Any, Any>(
                        java.util.function.Function<Any, Any> { java.util.Map.Entry.key },
                        java.util.function.Function<Any, Any> { entry: Map.Entry<error.NonExistentClass, Pageable<Comment?>> ->
                            pool.submit(
                                java.util.concurrent.Callable<T> {
                                    val paging: Paging? = entry.value.pastPage()
                                    entry.key.getComments(paging)
                                })
                        })
                )

        val entities: Map<CommentsRequest, Pageable<Comment>> = futures
            .entries.stream().collect<Map<CommentsRequest, Pageable<Comment>>, Any>(
                java.util.stream.Collectors.toMap<Any, Any, Any>(
                    java.util.function.Function<Any, Any> { java.util.Map.Entry.key },
                    java.util.function.Function<Any, Any> { entry: Map.Entry<CommentsRequest?, java.util.concurrent.Future<Pageable<Comment?>?>> ->
                        HandlingUtil.runtime(
                            kotlin.jvm.functions.Function0<out T?> { entry.value.get() })
                    })
            )

        model.setEntities(entities)
        model.setSinceDateFromEntities()
        model.margeWhenPastPageRequest(commentGroup)
        model.requestGroup = commentGroup.requestGroup
        return model
    }
}
