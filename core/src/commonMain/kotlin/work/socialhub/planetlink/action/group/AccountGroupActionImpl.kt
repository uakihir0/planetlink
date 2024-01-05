package net.socialhub.planetlink.action.group

import net.socialhub.planetlink.action.request.CommentsRequest
import net.socialhub.planetlink.model.User
import net.socialhub.planetlink.model.group.CommentGroup
import net.socialhub.planetlink.model.group.UserGroup

/**
 * グループアクション
 */
class AccountGroupActionImpl(accountGroup: AccountGroup) : AccountGroupAction {
    private val accountGroup: AccountGroup = accountGroup

    override val userMe: UserGroup?
        /**
         * {@inheritDoc}
         */
        get() {
            val model: UserGroupImpl = UserGroupImpl()
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

    override val homeTimeLine: CommentGroup?
        /**
         * {@inheritDoc}
         */
        get() {
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
