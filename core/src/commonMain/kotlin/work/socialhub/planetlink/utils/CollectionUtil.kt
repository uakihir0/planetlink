package work.socialhub.planetlink.utils

object CollectionUtil {

    /**
     * Get Partitioned List
     * リストを分割
     */
    fun <T> List<T>.partitionList(length: Int): List<List<T>> {
        val results = mutableListOf(mutableListOf<T>())
        for (i in this.indices) {
            if ((i % length) == 0) {
                results.add(mutableListOf())
            }
            results[results.size - 1].add(this[i])
        }
        return results
    }
}
