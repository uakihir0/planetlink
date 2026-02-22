package work.socialhub.planetlink.action.group

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.group.*

/**
 * グループアクション
 */
class AccountGroupActionImpl(
    private val accountGroup: AccountGroup
) : AccountGroupAction {

    /**
     * {@inheritDoc}
     */
    override suspend fun userMe(): UserGroup {
        return UserGroupImpl(
            coroutineScope {
                mutableMapOf<Account, User>().also { map ->
                    accountGroup.accounts.map { acc ->
                        async { map[acc] = acc.action.userMe() }
                    }.joinAll()
                }
            }
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun homeTimeLine(): CommentGroup {
        return comments(
            accountGroup.accounts.map {
                it.action.request().homeTimeLine()
            })
    }

    private suspend fun comments(
        requests: List<CommentsRequest>
    ): CommentGroup {
        return CommentsRequestGroupImpl(*requests.toTypedArray())
            .action().comments()
    }
}
