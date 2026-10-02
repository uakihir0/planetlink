package work.socialhub.planetlink.saypip.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import work.socialhub.ksaypip.entity.Conversation
import work.socialhub.ksaypip.entity.IdentifiedPage
import work.socialhub.ksaypip.entity.IdentifiedPerson
import work.socialhub.ksaypip.entity.Mark
import work.socialhub.ksaypip.entity.Media
import work.socialhub.ksaypip.entity.Me
import work.socialhub.ksaypip.entity.Participant
import work.socialhub.ksaypip.entity.Person
import work.socialhub.ksaypip.entity.Post
import work.socialhub.ksaypip.entity.PostConversations
import work.socialhub.ksaypip.entity.PostLastReply
import work.socialhub.ksaypip.entity.PostReaction
import work.socialhub.ksaypip.entity.Profile
import work.socialhub.ksaypip.entity.QuotedPost
import work.socialhub.ksaypip.entity.Relationship
import work.socialhub.ksaypip.entity.Reply
import work.socialhub.ksaypip.entity.UserPage
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.saypip.model.SaypipComment
import work.socialhub.planetlink.saypip.model.SaypipMe
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
                mark.colors = arrayOf("mint", "sage")
            }
            it.profile = Profile().also { profile ->
                profile.displayName = "Someone"
                profile.bio = "hello"
            }
        }

        val user = SaypipMapper.user(person, service())

        assertEquals("vi_tok_1", user.identityToken)
        assertNull(user.identifiedHandle)
        assertEquals("the game person", user.name)
        assertEquals("🐢", user.markEmoji)
        assertEquals(listOf("mint", "sage"), user.markColors)
        assertEquals("https://saypip.app/users/vi_tok_1", user.webUrl)
        assertEquals("vi_tok_1", user.accountIdentify)
    }

    @Test
    fun mapsAnIdentifiedPersonToAPublicUser() {
        val person = Person().also {
            it.identity = null
            it.identified = IdentifiedPerson().also { identified ->
                identified.handle = "foo"
                identified.displayName = "Foo"
                identified.avatarUrl = "https://saypip.app/api/media/md_1"
                identified.operator = true
            }
        }

        val user = SaypipMapper.user(person, service())

        assertEquals("", user.identityToken)
        assertEquals("foo", user.identifiedHandle)
        assertEquals("foo", user.accountIdentify)
        assertEquals("Foo", user.name)
        assertTrue(user.verified)
        assertTrue(user.operator)
        assertEquals("https://saypip.app/identified/foo", user.webUrl)
    }

    @Test
    fun mapsAnIdentifiedPageToThePersona() {
        val page = IdentifiedPage().also {
            it.handle = "foo"
            it.operator = true
            it.watching = true
            it.profile = Profile().also { profile ->
                profile.displayName = "Foo"
                profile.bio = "hello"
                profile.avatarUrl = "https://saypip.app/api/media/md_1"
            }
        }

        val user = SaypipMapper.user(page, service())

        assertEquals("foo", user.id?.value<String>())
        assertTrue(user.identityToken.isEmpty())
        assertEquals("foo", user.identifiedHandle)
        assertEquals("Foo", user.name)
        assertTrue(user.verified)
        assertTrue(user.operator)
        assertTrue(user.watching)
        assertEquals("https://saypip.app/identified/foo", user.webUrl)
    }

    @Test
    fun mapsAUserPageWithItsLabelAndWatch() {
        val page = UserPage().also {
            it.person = Person().also { person ->
                person.identity = "vi_tok_1"
            }
            it.watching = true
            it.note = "a memo"
            it.relationship = Relationship().also { relationship ->
                relationship.friendSince = "2026-08-19T09:00:00.000Z"
            }
        }

        val user = SaypipMapper.user(page, service())

        assertTrue(user.watching)
        assertEquals("a memo", user.note)
        assertEquals("2026-08-19T09:00:00.000Z", user.friendSince)
        assertTrue(user.relationship?.following == true)
    }

    @Test
    fun mapsTheAccountStateToMe() {
        val me = Me().also {
            it.profile = Profile().also { profile ->
                profile.displayName = "me"
            }
            it.unreadNotifications = 2
            it.unreadConversations = 1
            it.incomingFriendRequests = 3
            it.hasFriends = true
            it.hasWatches = true
            it.canPostIdentified = true
            it.wantsTalkPostId = "p_1"
            it.pinnedSubjects = arrayOf("猫", "本")
            it.isAdmin = false
            it.canSendFeedback = true
        }

        val mapped = SaypipMapper.me(me, service())

        assertTrue(mapped is SaypipMe)
        assertEquals("me", mapped.id?.value<String>())
        assertEquals(2, mapped.unreadNotifications)
        assertEquals(1, mapped.unreadConversations)
        assertEquals(3, mapped.incomingFriendRequests)
        assertTrue(mapped.hasFriends)
        assertTrue(mapped.hasWatches)
        assertTrue(mapped.canPostIdentified)
        assertEquals("p_1", mapped.wantsTalkPostId)
        assertEquals(listOf("猫", "本"), mapped.pinnedSubjects)
        assertEquals(false, mapped.isAdmin)
        assertTrue(mapped.canSendFeedback)
    }

    @Test
    fun mapsAPostWithReactionsAndQuotation() {
        val post = Post().also {
            it.id = "p_1"
            it.body = "hello"
            it.createdAt = "2026-08-19T09:00:00.000Z"
            it.author = null
            it.authorColors = arrayOf("sage", "mint")
            it.identified = true
            it.everyone = false
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
        assertEquals(listOf("sage", "mint"), comment.authorColors)
        assertTrue(comment.identified)
        assertEquals(false, comment.everyone)
        assertNull(comment.replyId)
        assertNotNull(comment.sharedComment)
        assertEquals("an older thought", comment.sharedComment?.text?.displayText)
        assertNull(comment.user)
    }

    @Test
    fun mapsAConversationReplyWithItsIdentifiedAuthorAndReactions() {
        val reply = Reply().also {
            it.id = "r_1"
            it.body = "hi"
            it.createdAt = "2026-08-19T09:00:00.000Z"
            it.side = "b"
            it.identified = true
            it.identifiedAuthor = IdentifiedPerson().also { identified ->
                identified.handle = "foo"
                identified.displayName = "Foo"
            }
            it.reactions = arrayOf(
                PostReaction().also { reaction ->
                    reaction.emoji = "🎉"
                    reaction.count = 1
                    reaction.mine = true
                },
            )
        }
        val conversation = Conversation().also { c ->
            c.id = "cv_1"
            c.participants = arrayOf(
                Participant().also { p ->
                    p.side = "a"
                    p.person = Person().also { it.identity = "vi_tok_1" }
                },
            )
        }

        val comment = SaypipMapper.reply(reply, conversation, service())

        assertTrue(comment is SaypipComment)
        assertEquals(true, comment.directMessage)
        assertEquals("r_1", comment.replyId)
        assertEquals("r_1", comment.id?.value<String>())
        assertEquals("Foo", comment.user?.name)
        assertEquals("foo", (comment.user as SaypipUser).identifiedHandle)
        assertTrue(comment.identified)
        // The reply's own bar, and no zero-count conversation entry from the post's getter.
        assertEquals(1, comment.reactions.size)
        assertEquals("🎉", comment.reactions[0].name)
        assertEquals(1, comment.reactions[0].count)
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
        assertNull((mapped.comments?.get(0) as? SaypipComment)?.replyId)
    }

    @Test
    fun mapsAReplyReactionLineToTheReply() {
        val notification = work.socialhub.ksaypip.entity.Notification().also {
            it.kind = "reply.reaction"
            it.conversationId = "cv_9"
            it.replyId = "r_1"
            it.replyBody = "my reply"
            it.peopleCount = 1
            it.arrivedAt = "2026-08-19T09:00:00.000Z"
        }

        val mapped = SaypipMapper.notification(notification, service())

        assertEquals("reply.reaction", mapped.type)
        assertEquals(NotificationActionType.REACTION.code, mapped.action)
        assertEquals("cv_9", mapped.comments?.get(0)?.id<String>())
        assertEquals("my reply", mapped.comments?.get(0)?.text?.displayText)
        assertEquals("r_1", (mapped.comments?.get(0) as? SaypipComment)?.replyId)
        assertEquals(true, mapped.comments?.get(0)?.directMessage)
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
        assertEquals(true, mapped.comments?.get(0)?.directMessage)
        assertNull((mapped.comments?.get(0) as? SaypipComment)?.replyId)
    }
}
