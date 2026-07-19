package work.socialhub.planetlink.x.define

import kotlin.js.JsExport
import work.socialhub.planetlink.define.action.ActionType

@JsExport
enum class XActionType : ActionType {
    RecommendedTimeLine,
    GetTrends,
    GetTrendLocations,
}
