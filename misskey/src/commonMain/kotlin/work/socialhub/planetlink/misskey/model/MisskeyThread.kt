package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread
import kotlin.js.JsExport

@JsExport
class MisskeyThread(
    service: Service
) : Thread(service) {

    /** グループスレッドかどうか？ */
    var isGroup: Boolean = false
}
