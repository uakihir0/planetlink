package work.socialhub.planetlink.utils

class MemoSupplier<T>(
    private var supplier: () -> T
) {
    private var cache: T? = null

    fun get(): T {
        return cache ?: run {
            return supplier.invoke()
                .also { this.cache = it }
        }
    }

    companion object {
        fun <K> of(supplier: () -> K): MemoSupplier<K> {
            return MemoSupplier(supplier)
        }
    }
}
