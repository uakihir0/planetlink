package work.socialhub.planetlink.action.group

import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.group.CommentGroup
import work.socialhub.planetlink.model.group.CommentGroupImpl
import net.socialhub.planetlink.model.group.CommentsRequestGroupImpl
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging

class CommentsRequestGroupActionImpl(
    private val requestGroup: CommentsRequestGroupImpl
) : CommentsRequestGroupAction {


    /**
     * {@inheritDoc}
     */
    override fun comments(
        count: Int
    ): CommentGroup {
        return comments(Paging(count))
    }

    /**
     * コメント情報をページング付きで取得
     */
    fun comments(
        paging: Paging
    ): CommentGroup {

        val model = CommentGroupImpl()
        val pool: ExecutorService = Executors.newCachedThreadPool()
        val copiedPage: Paging? = if ((paging != null)) paging.copy() else null

        val futures: Map<CommentsRequest, java.util.concurrent.Future<Pageable<Comment>>> = requestGroup //
            .getRequests().stream().collect(
                java.util.stream.Collectors.toMap(java.util.function.Function.identity<Any>(),  //
                    java.util.function.Function<Any, java.util.concurrent.Future<Any>> { request: Any ->
                        pool.submit(
                            java.lang.Runnable { request.getComments(copiedPage) })
                    })
            )

        val entities: Map<CommentsRequest, Pageable<Comment>> = futures //
            .entries.stream().collect<Map<CommentsRequest, Pageable<Comment>>, Any>(
                java.util.stream.Collectors.toMap<Any, Any, Any>(
                    java.util.function.Function<Any, Any> { java.util.Map.Entry.key },  //
                    java.util.function.Function<Any, Any> { entry: Map.Entry<CommentsRequest?, java.util.concurrent.Future<Pageable<Comment?>?>> ->
                        HandlingUtil.runtime(
                            kotlin.jvm.functions.Function0<out T?> { entry.value.get() })
                    })
            )

        model.setEntities(entities)
        model.setMaxDate(java.util.Date())
        model.setSinceDateFromEntities()
        model.requestGroup = requestGroup
        return model
    }
}
