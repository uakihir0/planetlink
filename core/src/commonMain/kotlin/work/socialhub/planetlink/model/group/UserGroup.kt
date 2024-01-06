package net.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.UserGroupAction
import work.socialhub.planetlink.model.User

interface UserGroup {
    /**
     * Return User related to Accounts
     * アカウントに紐づくユーザーを返す
     */
    val entities: Map<Any?, User?>?

    /**
     * Get Actions
     */
    fun action(): UserGroupAction?
}
