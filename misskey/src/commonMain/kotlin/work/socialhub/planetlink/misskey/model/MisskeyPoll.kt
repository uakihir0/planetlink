package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.model.Poll
import work.socialhub.planetlink.model.Service

class MisskeyPoll(
    service: Service
) : Poll(service) {

    /** 親ノートの ID 情報  */
    var noteId: String? = null
}
