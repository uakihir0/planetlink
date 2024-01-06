package net.socialhub.planetlink.action

import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.User

class UserActionImpl(action: AccountAction) : UserAction {
    private val action: AccountAction = action
    private var user: User? = null

    fun user(user: User?): UserAction {
        this.user = user
        return this
    }

    /**
     * {@inheritDoc}
     */
    override fun refresh(): User {
        return action.getUser(user)
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

    override val relationship: Relationship?
        /**
         * {@inheritDoc}
         */
        get() = action.getRelationship(user)
}

