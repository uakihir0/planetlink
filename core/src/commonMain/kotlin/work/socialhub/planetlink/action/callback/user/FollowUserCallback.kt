package work.socialhub.planetlink.action.callback.user

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.event.UserEvent
import kotlin.js.JsExport

@JsExport
interface FollowUserCallback : EventCallback {
    // See EventCallback.kt for why companion object is needed
    companion object
    fun onFollow(event: UserEvent?)
}
