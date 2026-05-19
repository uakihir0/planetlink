package work.socialhub.planetlink.model.support

import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Trend
import kotlin.js.JsExport

@JsExport
class TrendComment(
    var trend: Trend?,
    var comment: Comment?,
)
