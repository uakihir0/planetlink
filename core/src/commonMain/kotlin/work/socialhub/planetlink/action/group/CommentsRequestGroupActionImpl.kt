package net.socialhub.planetlink.action.group

import net.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Pageable
import net.socialhub.planetlink.model.group.CommentGroup
import work.socialhub.planetlink.model.Comment

class CommentsRequestGroupActionImpl(requestGroup: CommentsRequestGroupImpl) : CommentsRequestGroupAction {
    private val requestGroup: CommentsRequestGroupImpl

    init {
        this.requestGroup = requestGroup
    }

    override val comments: CommentGroup?
        /**
         * {@inheritDoc}
         */
        get() = getComments(Paging(200L))

    /**
     * {@inheritDoc}
     */
    override fun getComments(count: Int): CommentGroup {
        return getComments(Paging(count.toLong()))
    }

    /**
     * コメント情報をページング付きで取得
     */
    override fun getComments(paging: Paging?): CommentGroup {
        val model: CommentGroupImpl = CommentGroupImpl()
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
