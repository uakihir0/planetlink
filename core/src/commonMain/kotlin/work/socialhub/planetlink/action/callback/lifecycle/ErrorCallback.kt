package work.socialhub.planetlink.action.callback.lifecycle

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.model.error.SocialHubException
import kotlin.js.JsExport

@JsExport
interface ErrorCallback : EventCallback {
    fun onError(e: SocialHubException)
}