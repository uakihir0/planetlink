package work.socialhub.planetlink.action.group

import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import work.socialhub.planetlink.model.group.UserGroup
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.group.AccountGroup
import work.socialhub.planetlink.model.group.CommentGroup
import work.socialhub.planetlink.model.group.CommentsRequestGroupImpl
import work.socialhub.planetlink.model.group.UserGroupImpl

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
        return UserGroupImpl(
            runBlocking {
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
    override fun homeTimeLine(): CommentGroup {
        return comments(
            accountGroup.accounts.map {
                it.action.request().homeTimeLine()
            })
    }

    private fun comments(
        requests: List<CommentsRequest>
    ): CommentGroup {
        return CommentsRequestGroupImpl(*requests.toTypedArray())
            .action().comments()
    }
}
