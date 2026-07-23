package work.socialhub.planetlink.misskey.action

import kotlinx.coroutines.test.runTest
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.kmisskey.MisskeyFactory
import work.socialhub.kmisskey.entity.Reaction as MisskeyReaction
import work.socialhub.kmisskey.stream.MisskeyStream
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.misskey.model.MisskeyComment
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class MisskeyCommentUpdateStreamTest {

    @Test
    fun appliesReactionToCopyAndStopsAfterRemoval() = runTest {
        val account = Account()
        val service = Service("misskey", account)
        account.service = service
        val updates = mutableListOf<MisskeyComment>()
        val callback = object : UpdateCommentCallback {
            override fun onUpdate(event: CommentEvent?) {
                updates.add(event!!.comment as MisskeyComment)
            }
        }
        val original = MisskeyComment(service).also { comment ->
            comment.id = ID("note-1")
            comment.requesterHost = "example.com"
            comment.reactions = listOf(Reaction().also {
                it.name = "like"
                it.count = 1
            })
        }
        val stream = MisskeyCommentUpdateStream(
            stream = MisskeyStream(
                MisskeyFactory.instance("https://example.com", "token")
            ),
            callback = callback,
            service = service,
            host = "example.com",
            meId = "me",
        )

        stream.addComments(listOf(original))
        stream.handleReaction(
            MisskeyReaction().also {
                it.noteId = "note-1"
                it.userId = "me"
                it.reaction = "like"
            },
            increment = true,
        )

        val updated = updates.single()
        assertNotSame(original, updated)
        assertEquals(2, updated.reactions.first { it.name == "like" }.count)
        assertTrue(updated.reactions.first { it.name == "like" }.reacting)
        assertEquals(1, original.reactions.first { it.name == "like" }.count)

        stream.removeComments(listOf(original))
        updates.clear()
        stream.handleReaction(
            MisskeyReaction().also {
                it.noteId = "note-1"
                it.userId = "someone"
                it.reaction = "like"
            },
            increment = true,
        )
        assertTrue(updates.isEmpty())
    }
}
