package work.socialhub.planetlink.model.group

import net.socialhub.planetlink.model.group.UserGroup
import work.socialhub.planetlink.action.group.UserGroupAction
import work.socialhub.planetlink.model.User

class UserGroupImpl : UserGroup {
    //endregion
    //region // Getter&Setter
    override var entities: Map<Account, User>? = null

    override fun action(): UserGroupAction {
        return UserGroupActionImpl(this)
    }
}
