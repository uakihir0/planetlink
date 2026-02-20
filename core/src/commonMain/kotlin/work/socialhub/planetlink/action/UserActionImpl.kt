package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.User

class UserActionImpl(
    var action: AccountAction,
    var user: User,
) : UserAction {

    /**
     * {@inheritDoc}
     */
    override suspend fun userRefresh(): User {
        return action.user(user)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun follow() {
        action.followUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unfollow() {
        action.unfollowUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun mute() {
        action.muteUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unmute() {
        action.unmuteUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun block() {
        action.blockUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unblock() {
        action.unblockUser(user)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun relationship(): Relationship {
        return action.relationship(user)
    }
}
