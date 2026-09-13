package work.socialhub.planetlink.saypip.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import work.socialhub.ksaypip.entity.Mark
import work.socialhub.ksaypip.entity.Media
import work.socialhub.ksaypip.entity.Person
import work.socialhub.ksaypip.entity.Post
import work.socialhub.ksaypip.entity.PostConversations
import work.socialhub.ksaypip.entity.PostLastReply
import work.socialhub.ksaypip.entity.PostReaction
import work.socialhub.ksaypip.entity.Profile
import work.socialhub.ksaypip.entity.QuotedPost
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.saypip.model.SaypipComment
import work.socialhub.planetlink.saypip.model.SaypipUser

class SaypipMapperTest {

    private fun service(): Service {
        return Service("saypip", Account()).also {
            it.host = "https://saypip.app"
        }
    }

    @Test
    fun mapsAPersonToAViewerScopedUser() {
        val person = Person().also {
            it.identity = "vi_tok_1"
            it.label = "the game person"
            it.mark = Mark().also { mark ->
                mark.emoji = "🐢"
                mark.color = "mint"
            }
            it.profile = Profile().also { profile ->
                profile.displayName = "Someone"
                profile.bio = "hello"
            }
        }

        val user = SaypipMapper.user(person, service())

        assertEquals("vi_tok_1", user.identityToken)
        assertEquals("the game person", user.name)
        assertEquals("🐢", user.markEmoji)
        assertEquals("mint", user.markColor)
        assertEquals("https://saypip.app/users/vi_tok_1", user.webUrl)
        assertEquals("vi_tok_1", user.accountIdentify)
    }

    @Test
    fun mapsAPostWithReactionsAndQuotation() {
        val post = Post().also {
            it.id = "p_1"
            it.body = "hello"
            it.createdAt = "2026-08-19T09:00:00.000Z"
            it.author = null
            it.authorColor = "sage"
            it.readableUntil = "2026-08-26T09:00:00.000Z"
            it.wantsTalk = true
            it.media = arrayOf(
                Media().also { media ->
                    media.id = "md_1"
                    media.url = "https://saypip.app/api/media/md_1"
                    media.thumbnailUrl = "https://saypip.app/api/media/md_1?variant=thumb"
                    media.width = 1600
                    media.height = 900
                    media.alt = "a picture"
                },
            )
            it.reactions = arrayOf(
                PostReaction().also { reaction ->
                    reaction.emoji = "🎉"
                    reaction.count = 3
                    reaction.mine = true
                },
            )
            it.conversations = PostConversations().also { conversations ->
                conversations.count = 2
                conversations.mine = false
                conversations.lastReply = PostLastReply().also { last ->
                    last.body = "hi"
                    last.side = "b"
                }
            }
            it.replyTo = QuotedPost().also { quoted ->
                quoted.id = "p_0"
                quoted.body = "an older thought"
                quoted.createdAt = "2026-08-10T09:00:00.000Z"
            }
        }

        val comment = SaypipMapper.comment(post, service())

        assertTrue(comment is SaypipComment)
        assertEquals("hello", comment.text?.displayText)
        assertEquals("2026-08-19T09:00:00Z", comment.createAt.toString())
        assertEquals(1, comment.medias.size)
        assertEquals("a picture", comment.medias[0].description)
        assertEquals(1600, comment.medias[0].width)
        // The bar itself plus the derived conversation count.
        assertEquals(2, comment.reactions.size)
        assertEquals("🎉", comment.reactions[0].name)
        assertEquals(3, comment.reactions[0].count)
        assertTrue(comment.reactions[0].reacting)
        val conversation = comment.reactions.first { it.name == "conversation" }
        assertEquals(2, conversation.count)
        assertEquals(2, comment.conversationCount)
        assertEquals("b", comment.conversationLastSide)
        assertTrue(comment.wantsTalk)
        assertEquals("2026-08-26T09:00:00.000Z", comment.readableUntil)
        assertNotNull(comment.sharedComment)
        assertEquals("an older thought", comment.sharedComment?.text?.displayText)
        assertNull(comment.user)
    }

    @Test
    fun mapsAReactionLineToAQuoteOfYourOwnPost() {
        val notification = work.socialhub.ksaypip.entity.Notification().also {
            it.kind = "post.reaction"
            it.postId = "p_1"
            it.postBody = "my post"
            it.peopleCount = 2
            it.arrivedAt = "2026-08-19T09:00:00.000Z"
        }

        val mapped = SaypipMapper.notification(notification, service())

        assertEquals("post.reaction", mapped.type)
        assertEquals(NotificationActionType.REACTION.code, mapped.action)
        assertEquals(1, mapped.comments?.size)
        assertEquals("my post", mapped.comments?.get(0)?.text?.displayText)
    }

    @Test
    fun mapsAReplyLineToItsConversation() {
        val notification = work.socialhub.ksaypip.entity.Notification().also {
            it.kind = "conversation.reply"
            it.conversationId = "c_1"
            it.body = "an answer"
            it.arrivedAt = "2026-08-19T09:00:00.000Z"
        }

        val mapped = SaypipMapper.notification(notification, service())

        assertEquals("conversation.reply", mapped.type)
        assertEquals(NotificationActionType.MENTION.code, mapped.action)
        assertEquals(1, mapped.comments?.size)
        assertEquals("c_1", mapped.comments?.get(0)?.id<String>())
        assertTrue(mapped.comments?.get(0)?.directMessage == true)
    }
}
