package work.socialhub.planetlink.action.callback.lifecycle

import work.socialhub.planetlink.action.callback.EventCallback

interface ErrorCallback: EventCallback {
    fun onError(e: Exception)
}