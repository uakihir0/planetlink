package work.socialhub.planetlink.action.callback.user

import net.socialhub.planetlink.model.event.UserEvent
import work.socialhub.planetlink.action.callback.EventCallback

interface FollowUserCallback : EventCallback {
    fun onFollow(event: UserEvent?)
}
