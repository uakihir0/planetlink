package work.socialhub.planetlink.model

/**
 * Identify
 * 識別
 */
open class Identify(
    var service: Service
) {
    var id: ID? = null

    inline fun <reified T> id(): T {
        return checkNotNull(id) {
            "id is null."
        }.value<T>()
    }

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
