package work.socialhub.planetlink.model.event

import work.socialhub.planetlink.model.User
import kotlin.js.JsExport

@JsExport
class UserEvent(
    var user: User
)
