package work.socialhub.planetlink.action

abstract class ServiceAuth<T> {
    abstract val accessor: T

    /** it calls when token is refreshed. */
    var tokenRefreshCallback: (() -> Unit)? = null
}
