package work.socialhub.planetlink.action.callback.lifecycle

import work.socialhub.planetlink.action.callback.EventCallback
import kotlin.js.JsExport

@JsExport
interface ErrorCallback : EventCallback {
    // See EventCallback.kt for why companion object is needed
    companion object
    fun onError(e: Exception)
}