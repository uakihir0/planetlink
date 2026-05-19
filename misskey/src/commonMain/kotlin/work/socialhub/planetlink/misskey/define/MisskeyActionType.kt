package work.socialhub.planetlink.misskey.define

import work.socialhub.planetlink.define.action.ActionType
import kotlin.js.JsExport

@JsExport
enum class MisskeyActionType : ActionType {

    // TimeLine
    LocalTimeLine,
    FederationTimeLine,
    FeaturedTimeline,
}