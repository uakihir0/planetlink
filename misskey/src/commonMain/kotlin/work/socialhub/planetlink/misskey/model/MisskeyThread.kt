package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread

class MisskeyThread(
    service: Service
) : Thread(service) {

    /** グループスレッドかどうか？ */
    var isGroup: Boolean = false
}
