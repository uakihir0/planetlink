package work.socialhub.planetlink.model.support

import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service

class TupleIdentify(
    service: Service
) : Identify(service) {
    var subId: ID? = null
}
