package net.socialhub.planetlink.action.callback.lifecycle

import net.socialhub.planetlink.action.callback.EventCallback

interface DisconnectCallback : EventCallback {
    fun onDisconnect()
}
