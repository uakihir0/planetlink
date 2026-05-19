package work.socialhub.planetlink.model.support

import kotlin.js.JsExport

@JsExport
class TrendCountry {
    var id: Int? = null
    var name: String? = null
    var locations: List<TrendLocation>? = null

    class TrendLocation {
        var id: Int? = null
        var name: String? = null
    }
}
