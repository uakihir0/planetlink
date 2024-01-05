package net.socialhub.planetlink.action.callback.user

import net.socialhub.planetlink.action.callback.EventCallback
import net.socialhub.planetlink.model.event.UserEvent

interface FollowUserCallback : EventCallback {
    fun onFollow(event: UserEvent?)
}
