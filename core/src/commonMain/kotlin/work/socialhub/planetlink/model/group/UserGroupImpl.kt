package work.socialhub.planetlink.model.group

import work.socialhub.planetlink.action.group.UserGroupAction
import work.socialhub.planetlink.action.group.UserGroupActionImpl
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.User

class UserGroupImpl(
    override var entities: Map<Account, User>
) : UserGroup {

    override fun action(): UserGroupAction {
        return UserGroupActionImpl(this)
    }
}
