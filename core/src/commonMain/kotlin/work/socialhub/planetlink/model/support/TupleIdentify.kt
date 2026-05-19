package work.socialhub.planetlink.model.support

import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service
import kotlin.js.JsExport

@JsExport
class TupleIdentify(
    service: Service
) : Identify(service) {
    var subId: ID? = null
}
