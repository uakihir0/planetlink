package net.socialhub.planetlink.action

import work.socialhub.planetlink.action.AccountAction
import work.socialhub.planetlink.action.UserAction
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.User

class UserActionImpl(
    var action: AccountAction,
    var user: User,
) : UserAction {

    /**
     * {@inheritDoc}
     */
    override fun userRefresh(): User {
        return action.user(user)
    }

    /**
     * {@inheritDoc}
     */
    override fun follow() {
        action.followUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override fun unfollow() {
        action.unfollowUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override fun mute() {
        action.muteUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override fun unmute() {
        action.unmuteUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override fun block() {
        action.blockUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override fun unblock() {
        action.unblockUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override fun relationship(): Relationship {
        return action.relationship(user)
    }
}

