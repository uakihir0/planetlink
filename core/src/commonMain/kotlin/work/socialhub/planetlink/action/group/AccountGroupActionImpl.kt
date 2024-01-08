package work.socialhub.planetlink.action.group

import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.group.AccountGroup
import work.socialhub.planetlink.model.group.CommentGroup
import net.socialhub.planetlink.model.group.UserGroup
import work.socialhub.planetlink.model.group.UserGroupImpl
import work.socialhub.planetlink.model.User

/**
 * グループアクション
 */
class AccountGroupActionImpl(
    private val accountGroup: AccountGroup
) : AccountGroupAction {

    /**
     * {@inheritDoc}
     */
    override fun userMe(): UserGroup {
        val model = UserGroupImpl()
        val pool: ExecutorService = Executors.newCachedThreadPool()

        val futures: Map<Account, java.util.concurrent.Future<User>> = accountGroup //
            .accounts.stream().collect<Map<Account, java.util.concurrent.Future<User>>, Any>(
                java.util.stream.Collectors.toMap(java.util.function.Function.identity<Any>(),  //
                    java.util.function.Function<Any, java.util.concurrent.Future<Any>> { acc: Any ->
                        pool.submit(
                            java.lang.Runnable { acc.action().getUserMe() })
                    })
            )

        val entities: Map<Account, User> = futures //
            .entries.stream().collect<Map<Account, User>, Any>(
                java.util.stream.Collectors.toMap<Any, Any, Any>(
                    java.util.function.Function<Any, Any> { java.util.Map.Entry.key },  //
                    java.util.function.Function<Any, Any> { entry: Map.Entry<Account?, java.util.concurrent.Future<User?>> ->
                        HandlingUtil.runtime(
                            kotlin.jvm.functions.Function0<out T?> { entry.value.get() })
                    })
            )

        model.entities = entities
        return model
    }

    /**
     * {@inheritDoc}
     */
    fun homeTimeLine(): CommentGroup {

        val requests: List<CommentsRequest> = accountGroup.accounts.stream() //
            .map<Any>(java.util.function.Function<Any, Any> { acc: Any ->
                acc.action().request().getHomeTimeLine()
            }) //
            .collect<List<CommentsRequest>, Any>(java.util.stream.Collectors.toList<Any>())
        return getComments(requests)
    }

    private fun getComments(requests: List<CommentsRequest>): CommentGroup {
        return CommentsRequestGroupImpl(requests.toArray(arrayOf<CommentsRequest>())) //
            .action().getComments()
    }
}
