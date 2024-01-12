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
}
