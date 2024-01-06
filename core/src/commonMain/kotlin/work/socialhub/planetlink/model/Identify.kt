package work.socialhub.planetlink.model

/**
 * Identify
 * 識別
 */
open class Identify(
    var service: Service
) {
    var id: Any? = null

    var serializedIdString: String
        /**
         * Get serialized string ID. (for serialize request)
         * (Get id string with type information.)
         * 識別情報の ID に型情報を入れて文字列として返却
         */
        get() {
            if (id is Int) {
                return "I" + id.toString()
            }
            if (id is Long) {
                return "L" + id.toString()
            }
            if (id is String) {
                return "S" + id.toString()
            }
            throw IllegalStateException("Not supported type.")
        }
        /**
         * Set serialized string ID. (for serialize request)
         * (Set id with typed id string.)
         * 型付き ID 文字列よりID情報を復元
         */
        set(idString) {
            if (idString.startsWith("I")) {
                id = idString.substring(1).toInt()
                return
            }
            if (idString.startsWith("L")) {
                id = idString.substring(1).toLong()
                return
            }
            if (idString.startsWith("S")) {
                id = idString.substring(1)
                return
            }
            throw IllegalStateException("Not supported type.")
        }

    /**
     * Is same identify?
     * 同じ識別子か？
     */
    fun isSameIdentify(id: Identify): Boolean {
        return ((service.type === id.service.type)
                && (this.id == id.id))
    }

    inline fun <reified T> id(): T? {
        if (id is T) return id as T
        return null
    }
}
