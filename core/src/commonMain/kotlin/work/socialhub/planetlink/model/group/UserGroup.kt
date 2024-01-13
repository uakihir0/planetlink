package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.UserGroupAction
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.User

interface UserGroup {

    /**
     * Return User related to Accounts
     * アカウントに紐づくユーザーを返す
     */
    val entities: Map<Account, User>

    /**
     * Get Actions
     */
    fun action(): UserGroupAction
}
