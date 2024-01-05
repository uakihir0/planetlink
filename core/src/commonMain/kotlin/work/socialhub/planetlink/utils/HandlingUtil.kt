package work.socialhub.planetlink.utils

object HandlingUtil {

    /**
     * Ignore Exceptions
     */
    fun <T> ignore(supplier: () -> T): T? {
        return try {
            supplier()
        } catch (ignore: Exception) {
            null
        }
    }

    /**
     * To Runtime Error
     */
    fun <T> runtime(supplier: () -> T): T {
        try {
            return supplier()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
