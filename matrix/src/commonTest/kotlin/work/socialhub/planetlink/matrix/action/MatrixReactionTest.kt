package work.socialhub.planetlink.matrix.action

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatrixReactionTest {

    @Test
    fun extractsReactionKeyFromRelationContent() {
        val content = mapOf(
            "m.relates_to" to JsonObject(
                mapOf(
                    "rel_type" to JsonPrimitive("m.annotation"),
                    "event_id" to JsonPrimitive("\$post"),
                    "key" to JsonPrimitive("\uD83D\uDC4D"),
                )
            )
        )

        assertEquals("\uD83D\uDC4D", matrixReactionKey(content))
    }

    @Test
    fun ignoresContentWithoutReactionRelation() {
        assertNull(matrixReactionKey(emptyMap()))
        assertNull(matrixReactionKey(mapOf("m.relates_to" to JsonPrimitive("invalid"))))
    }
}
