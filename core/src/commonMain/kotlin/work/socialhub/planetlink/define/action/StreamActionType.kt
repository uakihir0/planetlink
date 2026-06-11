package work.socialhub.planetlink.define.action

import kotlin.js.JsExport

@JsExport
enum class StreamActionType : ActionType {
    HomeTimeLineStream,
    NotificationStream,
}
