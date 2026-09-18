package work.socialhub.planetlink.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ContextTest {

    private val service = Service("test", Account())

    private fun comment(
        id: String,
        timestamp: Long?,
    ): Comment {
        return Comment(service).apply {
            this.id = ID(id)
            createAt = timestamp?.let(Instant::fromEpochMilliseconds)
        }
    }

    @Test
    fun sortsBothSidesOldestFirstWithStableTieBreakers() {
        val context = Context().apply {
            ancestors = listOf(
                comment("parent", 2_000),
                comment("root", 1_000),
                comment("same-time-b", 3_000),
                comment("same-time-a", 3_000),
            )
            descendants = listOf(
                comment("reply-b", 5_000),
                comment("reply-a", 5_000),
                comment("nested", 6_000),
            )
        }

        context.sort()

        assertEquals(
            listOf("root", "parent", "same-time-a", "same-time-b"),
            context.ancestors.orEmpty().map { it.id!!.value<String>() },
        )
        assertEquals(
            listOf("reply-a", "reply-b", "nested"),
            context.descendants.orEmpty().map { it.id!!.value<String>() },
        )
    }

    @Test
    fun sortsNullTimestampsLastAndAllowsMissingSides() {
        val context = Context().apply {
            ancestors = listOf(comment("missing", null), comment("known", 1_000))
        }

        context.sort()

        assertEquals(
            listOf("known", "missing"),
            context.ancestors.orEmpty().map { it.id!!.value<String>() },
        )
        assertNull(context.descendants)
    }
}
