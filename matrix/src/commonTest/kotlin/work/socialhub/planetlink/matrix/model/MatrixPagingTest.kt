package work.socialhub.planetlink.matrix.model

import kotlin.test.Test
import kotlin.test.assertEquals
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service

class MatrixPagingTest {

    private val comments = listOf(
        Comment(Service("matrix", Account())).apply { id = ID("event") }
    )

    @Test
    fun pastPageUsesTheResponseEndToken() {
        val paging = MatrixPaging().apply {
            from = "current-start"
            to = "older-start"
            direction = "b"
        }

        val next = paging.pastPage(comments) as MatrixPaging

        assertEquals("older-start", next.from)
        assertEquals(null, next.to)
        assertEquals("b", next.direction)
    }

    @Test
    fun newPageUsesTheCurrentStartToken() {
        val paging = MatrixPaging().apply {
            from = "current-start"
            to = "older-start"
            direction = "b"
        }

        val next = paging.newPage(comments) as MatrixPaging

        assertEquals("current-start", next.from)
        assertEquals(null, next.to)
        assertEquals("f", next.direction)
    }
}
