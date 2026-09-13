package work.socialhub.planetlink.saypip.action

import kotlin.time.Instant
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.saypip.model.SaypipComment
import work.socialhub.planetlink.saypip.model.SaypipPaging
import work.socialhub.planetlink.saypip.model.SaypipThread
import work.socialhub.planetlink.saypip.model.SaypipUser
import work.socialhub.ksaypip.entity.Conversation as SaypipConversation
import work.socialhub.ksaypip.entity.ConversationDigest as SaypipConversationDigest
import work.socialhub.ksaypip.entity.Feed as SaypipFeed
import work.socialhub.ksaypip.entity.Media as SaypipMedia
import work.socialhub.ksaypip.entity.Me as SaypipMe
import work.socialhub.ksaypip.entity.Notification as SaypipNotification
import work.socialhub.ksaypip.entity.NotificationList as SaypipNotificationList
import work.socialhub.ksaypip.entity.Person as SaypipPerson
import work.socialhub.ksaypip.entity.Post as SaypipPost
import work.socialhub.ksaypip.entity.PostReaction as SaypipPostReaction
import work.socialhub.ksaypip.entity.QuotedPost as SaypipQuotedPost
import work.socialhub.ksaypip.entity.Relationship as SaypipRelationship
import work.socialhub.ksaypip.entity.Reply as SaypipReply
import work.socialhub.ksaypip.entity.UserPage as SaypipUserPage

object SaypipMapper {

    /** The synthetic identity of the authenticated account, which has no token of its own. */
    const val MY_IDENTITY = "me"

    // ============================================================== //
    // Single Object Mapper
    // ============================================================== //
    /**
     * ユーザーマッピング
     */
    fun user(
        person: SaypipPerson,
        service: Service,
    ): SaypipUser {
        return SaypipUser(service).also { u ->
            u.id = ID(person.identity)
            u.identityToken = person.identity
            u.name = person.label ?: person.profile?.displayName ?: ""
            u.markEmoji = person.mark.emoji
            u.markColor = person.mark.color
            u.description = AttributedString.plain(person.profile?.bio ?: "")
            u.iconImageUrl = person.profile?.avatarUrl
            u.coverImageUrl = person.profile?.bannerUrl
            u.webUrl = "${service.host}/users/${person.identity}"
        }
    }

    /**
     * ユーザーページマッピング (関係を含む)
     */
    fun user(
        page: SaypipUserPage,
        service: Service,
    ): SaypipUser {
        return user(page.person, service).also { u ->
            u.friendSince = page.relationship?.friendSince
            u.relationship = relationship(page.relationship)
        }
    }

    /**
     * 自身のアカウントマッピング
     *
     * Saypip hands out no identity for the account itself — a viewer is the one constant of their
     * own view — so [MY_IDENTITY] stands in for it.
     */
    fun me(
        me: SaypipMe,
        service: Service,
    ): SaypipUser {
        return SaypipUser(service).also { u ->
            u.id = ID(MY_IDENTITY)
            u.identityToken = ""
            u.name = me.profile?.displayName ?: ""
            u.description = AttributedString.plain(me.profile?.bio ?: "")
            u.iconImageUrl = me.profile?.avatarUrl
            u.coverImageUrl = me.profile?.bannerUrl
            u.webUrl = "${service.host}/me"
        }
    }

    /**
     * 関係マッピング
     *
     * A relationship here is a friendship: mutual by construction, one flag for both directions.
     * Blocking and muting are not carried on the relationship page, so they stay false rather
     * than being guessed at.
     */
    fun relationship(
        relationship: SaypipRelationship?,
    ): Relationship {
        return Relationship().also { r ->
            val friends = relationship?.friendSince != null
            r.followed = friends
            r.following = friends
            r.blocking = false
            r.muting = false
        }
    }

    /**
     * コメントマッピング
     */
    fun comment(
        post: SaypipPost,
        service: Service,
    ): SaypipComment {
        return SaypipComment(service).also { c ->
            c.id = ID(post.id)
            c.text = AttributedString.plain(post.body)
            c.createAt = instant(post.createdAt)
            c.user = post.author?.let { user(it, service) }
            c.medias = post.media.map { media(it) }
            c.wantsTalk = post.wantsTalk
            c.readableUntil = post.readableUntil
            c.authorColor = post.authorColor
            c.reactions = post.reactions.map { reaction(it) }
            c.conversationCount = post.conversations.count
            c.conversationMine = post.conversations.mine
            c.conversationLastSide = post.conversations.lastReply?.side
            c.sharedComment = post.replyTo?.let { quotedComment(it, service) }
            c.webUrl = "${service.host}/posts/${post.id}"
        }
    }

    /**
     * 引用された投稿のマッピング (自己返信の引用)
     */
    fun quotedComment(
        post: SaypipQuotedPost,
        service: Service,
    ): SaypipComment {
        return SaypipComment(service).also { c ->
            c.id = ID(post.id)
            c.text = AttributedString.plain(post.body)
            c.createAt = instant(post.createdAt)
            c.medias = post.media.map { media(it) }
            c.readableUntil = post.readableUntil
            c.webUrl = "${service.host}/posts/${post.id}"
        }
    }

    /**
     * リアクションマッピング
     */
    fun reaction(
        reaction: SaypipPostReaction,
    ): Reaction {
        return Reaction().also { r ->
            r.name = reaction.emoji
            r.emoji = reaction.emoji
            r.count = reaction.count
            r.reacting = reaction.mine
        }
    }

    /**
     * メディアマッピング
     */
    fun media(
        media: SaypipMedia,
    ): Media {
        return Media().also { m ->
            m.type = MediaType.Image
            m.sourceUrl = media.url
            m.previewUrl = media.thumbnailUrl
            m.width = media.width
            m.height = media.height
            m.description = media.alt
        }
    }

    // ============================================================== //
    // List Object Mapper
    // ============================================================== //
    /**
     * タイムラインマッピング
     */
    fun timeLine(
        feed: SaypipFeed,
        service: Service,
        paging: Paging?,
    ): Pageable<Comment> {
        return Pageable<Comment>().also { p ->
            p.entities = feed.items
                .map { comment(it, service) }
                .sortedByDescending { it.createAt }

            val mpg = SaypipPaging.fromPaging(paging)
            mpg.nextCursor = feed.nextCursor
            p.paging = mpg
        }
    }

    /**
     * 会話の発言マッピング
     */
    fun comments(
        conversation: SaypipConversation,
        service: Service,
        paging: Paging?,
    ): Pageable<Comment> {
        return Pageable<Comment>().also { p ->
            p.entities = conversation.replies
                .map { reply(it, conversation, service) }
                .sortedByDescending { it.createAt }

            val mpg = SaypipPaging.fromPaging(paging)
            mpg.nextCursor = conversation.olderRepliesCursor
            p.paging = mpg
        }
    }

    /**
     * 通知マッピング
     */
    fun notification(
        notification: SaypipNotification,
        service: Service,
    ): Notification {
        return Notification(service).also { n ->
            n.id = ID(
                "${notification.kind}:" +
                    (notification.postId ?: notification.conversationId ?: notification.arrivedAt)
            )
            n.type = notification.kind
            n.action = when (notification.kind) {
                "post.reaction" -> NotificationActionType.REACTION.code
                else -> NotificationActionType.MENTION.code
            }
            n.createAt = instant(notification.arrivedAt)
            n.users = listOfNotNull(notification.person?.let { user(it, service) })

            // A reaction line quotes the reader's own post; a reply line leads to the
            // conversation rather than to any post.
            notification.postId?.let { postId ->
                n.comments = listOf(
                    SaypipComment(service).also { c ->
                        c.id = ID(postId)
                        c.text = AttributedString.plain(notification.postBody ?: "")
                        c.createAt = instant(notification.arrivedAt)
                        c.medias = notification.postImage?.let { listOf(media(it)) } ?: listOf()
                        c.webUrl = "${service.host}/posts/$postId"
                    },
                )
            } ?: notification.conversationId?.let { conversationId ->
                n.comments = listOf(
                    SaypipComment(service).also { c ->
                        c.id = ID(conversationId)
                        c.text = AttributedString.plain(notification.body ?: "")
                        c.createAt = instant(notification.arrivedAt)
                        c.directMessage = true
                    },
                )
            }
        }
    }

    /**
     * 通知一覧マッピング
     */
    fun notifications(
        list: SaypipNotificationList,
        service: Service,
        paging: Paging?,
    ): Pageable<Notification> {
        return Pageable<Notification>().also { p ->
            p.entities = list.items
                .map { notification(it, service) }
                .sortedByDescending { it.createAt }

            val mpg = SaypipPaging.fromPaging(paging)
            mpg.nextCursor = list.nextCursor
            p.paging = mpg
        }
    }

    /**
     * スレッド (会話) マッピング
     */
    fun thread(
        conversation: SaypipConversationDigest,
        service: Service,
    ): SaypipThread {
        return SaypipThread(service).also { t ->
            t.id = ID(conversation.id)
            t.users = conversation.participants
                .mapNotNull { it.person?.let { person -> user(person, service) } }
            t.lastUpdate = instant(conversation.lastReplyAt)
            t.description = conversation.lastReply?.body
            t.isMine = conversation.isMine
            t.unread = conversation.unread
        }
    }

    /**
     * 会話の一覧マッピング
     */
    fun threads(
        conversations: List<SaypipConversationDigest>,
        service: Service,
        paging: Paging?,
    ): Pageable<work.socialhub.planetlink.model.Thread> {
        return Pageable<work.socialhub.planetlink.model.Thread>().also { p ->
            p.entities = conversations
                .map { thread(it, service) }
                .sortedByDescending { it.lastUpdate }

            p.paging = SaypipPaging.fromPaging(paging)
        }
    }

    /**
     * 会話の発言 (返信) マッピング
     */
    fun reply(
        reply: SaypipReply,
        conversation: SaypipConversation,
        service: Service,
    ): Comment {
        val person = conversation.participants
            .firstOrNull { it.side == reply.side }
            ?.person

        return SaypipComment(service).also { c ->
            c.id = ID(reply.id)
            c.text = AttributedString.plain(reply.body)
            c.createAt = instant(reply.createdAt)
            c.user = person?.let { user(it, service) }
            c.directMessage = true
        }
    }

    private fun instant(
        value: String,
    ): Instant {
        return Instant.parse(value)
    }
}
