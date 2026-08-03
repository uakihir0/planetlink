package work.socialhub.planetlink.bluesky.define

import kotlin.js.JsExport
import work.socialhub.planetlink.define.action.ActionType

@JsExport
enum class BlueskyActionType : ActionType {
    GetCustomFeeds,
    CustomFeedTimeLine,
}
