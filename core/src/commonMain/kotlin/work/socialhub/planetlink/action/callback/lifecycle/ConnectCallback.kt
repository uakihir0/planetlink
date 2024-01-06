package work.socialhub.planetlink.action.callback.lifecycle

import work.socialhub.planetlink.action.callback.EventCallback

interface ConnectCallback : EventCallback {
    fun onConnect()
}
