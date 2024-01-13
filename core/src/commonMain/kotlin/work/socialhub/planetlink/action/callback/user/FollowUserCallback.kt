package work.socialhub.planetlink.action.callback.user

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.event.UserEvent

interface FollowUserCallback : EventCallback {
    fun onFollow(event: UserEvent?)
}
