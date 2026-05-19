package work.socialhub.planetlink.model

import kotlin.js.JsExport

/**
 * Identify
 * 識別
 */
@JsExport
open class Identify(
    var service: Service
) {
    var id: ID? = null

    @JsExport.Ignore
    inline fun <reified T> id(): T {
        return checkNotNull(id) {
            "id is null."
        }.value<T>()
    }

    @JsExport.Ignore
    constructor(
        service: Service,
        id: ID,
    ) : this(service) {
        this.id = id
    }

    /**
     * Is same identify?
     * 同じ識別子か？
     */
    fun isSameIdentify(id: Identify): Boolean {
        val aId = this.id
        val bId = id.id

        return ((aId != null && bId != null)
                && (service.type == id.service.type)
                && (aId.isSameID(bId)))
    }
}
