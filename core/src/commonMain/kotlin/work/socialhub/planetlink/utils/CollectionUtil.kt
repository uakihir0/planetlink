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

    /**
     * 特定の条件でない場合までのリストを取得
     */
    fun <T> List<T>.takeUntil(
        predicate: (T) -> Boolean
    ): List<T> {
        val result = mutableListOf<T>()
        for (item in this) {
            if (predicate(item)) {
                break
            }
            result.add(item)
        }
        return result
    }
}
