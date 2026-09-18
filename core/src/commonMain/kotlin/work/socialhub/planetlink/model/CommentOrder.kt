package work.socialhub.planetlink.model

/**
 * Stable ordering helpers for comment collections.
 *
 * SNS APIs do not all provide the same ordering guarantees, and several
 * platforms can return multiple comments with the same timestamp. Keep the
 * timestamp as the primary key and the platform identifier as a deterministic
 * tie-breaker.
 */
object CommentOrder {

    fun oldestFirst(comments: List<Comment>): List<Comment> {
        return comments.sortedWith(
            compareBy<Comment>(
                { it.createAt?.toEpochMilliseconds() ?: Long.MAX_VALUE },
                { it.id?.value?.toString() ?: "" },
            )
        )
    }

    fun newestFirst(comments: List<Comment>): List<Comment> {
        return comments.sortedWith(
            compareByDescending<Comment> {
                it.createAt?.toEpochMilliseconds() ?: Long.MIN_VALUE
            }.thenByDescending {
                it.id?.value?.toString() ?: ""
            }
        )
    }
}
