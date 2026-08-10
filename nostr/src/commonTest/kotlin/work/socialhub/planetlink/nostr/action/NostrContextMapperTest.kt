package work.socialhub.planetlink.nostr.action

import work.socialhub.knostr.entity.NostrEvent
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.social.model.NostrThread
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NostrContextMapperTest {

    @Test
    fun separatesAncestorsAndDescendantsWithoutSelectedNote() {
        val root = note("root", createdAt = 100)
        val parent = note("parent", parentId = "root", createdAt = 200)
        val selected = note("selected", parentId = "parent", createdAt = 300)
        val reply = note("reply", parentId = "selected", createdAt = 400)
        val nestedReply = note("nested-reply", parentId = "reply", createdAt = 500)
        val thread = NostrThread().apply {
            rootNote = selected
            replies = listOf(root, parent, selected, reply, nestedReply)
        }

        val context = NostrMapper.commentContext(thread, service())
        val ancestorIds = context.ancestors.orEmpty().map { it.id?.value<String>() }
        val descendantIds = context.descendants.orEmpty().map { it.id?.value<String>() }

        assertEquals(listOf("parent", "root"), ancestorIds)
        assertEquals(listOf("nested-reply", "reply"), descendantIds)
        assertFalse("selected" in ancestorIds)
        assertFalse("selected" in descendantIds)
    }

    @Test
    fun treatsRootMarkerAsParentForDirectReply() {
        val root = note("root", createdAt = 100)
        val selected = note(
            eventId = "selected",
            parentId = "root",
            marker = "root",
            createdAt = 200,
        )
        val reply = note("reply", parentId = "selected", createdAt = 300)
        val thread = NostrThread().apply {
            rootNote = selected
            replies = listOf(root, reply)
        }

        val context = NostrMapper.commentContext(thread, service())

        assertEquals(
            listOf("root"),
            context.ancestors.orEmpty().map { it.id?.value<String>() },
        )
        assertEquals(
            listOf("reply"),
            context.descendants.orEmpty().map { it.id?.value<String>() },
        )
    }

    private fun note(
        eventId: String,
        parentId: String? = null,
        marker: String = "reply",
        createdAt: Long,
    ): NostrNote {
        val tags = parentId?.let {
            listOf(listOf("e", it, "", marker))
        }.orEmpty()

        return NostrNote().apply {
            event = NostrEvent(
                id = eventId,
                pubkey = "author",
                createdAt = createdAt,
                kind = 1,
                tags = tags,
                content = eventId,
                sig = "signature",
            )
            content = event.content
            this.createdAt = event.createdAt
            noteId = eventId
        }
    }

    private fun service(): Service {
        val account = Account()
        return Service("nostr", account).also {
            account.service = it
        }
    }
}
