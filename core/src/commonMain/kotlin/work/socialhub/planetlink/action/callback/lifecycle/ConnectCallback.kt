package net.socialhub.planetlink.action.callback.lifecycle

import net.socialhub.planetlink.action.callback.EventCallback

interface ConnectCallback : EventCallback {
    fun onConnect()
}
