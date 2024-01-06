package work.socialhub.planetlink.action.callback.lifecycle

import work.socialhub.planetlink.action.callback.EventCallback

interface DisconnectCallback : EventCallback {
    fun onDisconnect()
}
