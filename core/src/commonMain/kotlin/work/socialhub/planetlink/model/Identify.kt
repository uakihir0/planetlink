package work.socialhub.planetlink.model

/**
 * Identify
 * 識別
 */
open class Identify(
    var service: Service
) {
    var id: ID? = null

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
