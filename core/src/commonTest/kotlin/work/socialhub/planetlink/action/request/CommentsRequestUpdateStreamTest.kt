package work.socialhub.planetlink.action.request

import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.action.StreamActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.CommentUpdateStream
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CommentsRequestUpdateStreamTest {

    @Test
    fun capabilityWiresDynamicUpdateStream() = runTest {
        val account = Account()
        account.service = Service("test", account)
        val action = FakeAction(account)
        account.action = action

        val request = action.request().homeTimeLine()
        assertTrue(request.canUseCommentsUpdateStream())

        val first = Comment(account.service).also { it.id = ID("first") }
        val second = Comment(account.service).also { it.id = ID("second") }
        val stream = request.setCommentsUpdateStream(
            listOf(first),
            object : EventCallback {},
        ) as FakeCommentUpdateStream

        assertEquals(1, stream.comments.size)
        assertSame(first, stream.comments.single())
        stream.addComments(listOf(second))
        assertEquals(2, stream.comments.size)
        assertSame(first, stream.comments[0])
        assertSame(second, stream.comments[1])
        stream.removeComments(listOf(first))
        assertEquals(1, stream.comments.size)
        assertSame(second, stream.comments.single())
        stream.open()
        assertTrue(stream.isOpened)
        stream.close()
        assertFalse(stream.isOpened)
        assertSame(stream, action.createdStream)
    }

    private class FakeAction(
        account: Account,
    ) : AccountActionImpl(account) {
        var createdStream: FakeCommentUpdateStream? = null

        override fun capabilities(): Capabilities {
            return Capabilities(setOf(StreamActionType.CommentUpdateStream))
        }

        override suspend fun setCommentUpdateStream(
            comments: List<Comment>,
            callback: EventCallback,
        ): CommentUpdateStream {
            return FakeCommentUpdateStream(comments.toMutableList())
                .also { createdStream = it }
        }
    }

    private class FakeCommentUpdateStream(
        val comments: MutableList<Comment>,
    ) : CommentUpdateStream {
        private var opened = false

        override val isOpened: Boolean
            get() = opened

        override suspend fun addComments(comments: List<Comment>) {
            this.comments.addAll(comments)
        }

        override suspend fun removeComments(comments: List<Comment>) {
            this.comments.removeAll(comments)
        }

        override suspend fun open() {
            opened = true
        }

        override fun close() {
            opened = false
        }
    }
}
