package work.socialhub.planetlink.mixi2.action

import kotlin.time.Instant
import work.socialhub.kmixi2web.entity.ChatRoom
import work.socialhub.kmixi2web.entity.ChatRoomMessage
import work.socialhub.kmixi2web.entity.Community
import work.socialhub.kmixi2web.entity.CommunityAccessLevel
import work.socialhub.kmixi2web.entity.Media as Mixi2Media
import work.socialhub.kmixi2web.entity.Notification as Mixi2RawNotification
import work.socialhub.kmixi2web.entity.NotificationActivityType
import work.socialhub.kmixi2web.entity.Persona
import work.socialhub.kmixi2web.entity.PersonaConnectivity
import work.socialhub.kmixi2web.entity.PersonaWithConnectivity
import work.socialhub.kmixi2web.entity.Post
import work.socialhub.kmixi2web.entity.Profile
import work.socialhub.kmixi2web.entity.Timestamp
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.mixi2.model.Mixi2Channel
import work.socialhub.planetlink.mixi2.model.Mixi2Comment
import work.socialhub.planetlink.mixi2.model.Mixi2Notification
import work.socialhub.planetlink.mixi2.model.Mixi2Paging
import work.socialhub.planetlink.mixi2.model.Mixi2Thread
import work.socialhub.planetlink.mixi2.model.Mixi2User

/**
 * mixi2 モデルの変換
 * mixi2 entity to PlanetLink model mapper.
 */
object Mixi2Mapper {

    /** The public web host of mixi2. */
    const val HOST = "https://mixi.social"

    // ============================================================== //
    // User
    // ============================================================== //
    /**
     * ペルソナのマッピング
     */
    fun user(
        persona: Persona,
        service: Service,
        connectivity: PersonaConnectivity? = null,
    ): Mixi2User {
        return Mixi2User(service).also { u ->
            u.id = ID(persona.personaId)
            u.personaId = persona.personaId
            u.handle = persona.name
            u.name = persona.displayName.ifBlank { persona.name }
            u.description = AttributedString.plain(persona.profileText)
            u.iconImageUrl = persona.avatarUrl.ifBlank { null }
            u.coverImageUrl = persona.profileImageUrl.ifBlank { null }
            u.statusText = persona.statusText
            u.statusIconUrl = persona.statusIcon?.icon?.ifBlank { null }
            u.verified = persona.verificationType != 0
            u.isFrozen = persona.isPersonaFrozen
            u.isBlocking = persona.isBlocking
            u.followingStatus = persona.followingStatus

            val following = connectivity?.following ?: persona.following
            val followed = connectivity?.followed ?: persona.followed
            if (connectivity != null || following || followed || persona.isBlocking) {
                u.relationship = Relationship().also { r ->
                    r.following = following
                    r.followed = followed
                    r.blocking = persona.isBlocking
                }
            }
        }
    }

    /**
     * プロフィールのマッピング
     */
    fun user(
        profile: Profile,
        service: Service,
    ): Mixi2User {
        val persona = profile.persona
            ?: throw IllegalStateException("The mixi2 profile carries no persona.")
        return user(persona, service, profile.personaConnectivity).also { u ->
            u.description = AttributedString.plain(
                profile.text.ifBlank { persona.profileText }
            )
            u.coverImageUrl = profile.profileImageUrl.ifBlank { null }
            u.followingCount = profile.followingCount
            u.followersCount = profile.followedCount
            u.link = profile.link.ifBlank { null }
            u.isMuted = profile.isMuted
            u.isBlocking = profile.isBlocking
            u.isBlocked = profile.isBlocked
            u.relationship = relationship(profile)
        }
    }

    /**
     * 関係のマッピング
     *
     * A profile read may leave the connectivity unset while the wrapped
     * persona still carries the follow state, so the persona is the fallback.
     */
    fun relationship(profile: Profile): Relationship {
        val persona = profile.persona
        return Relationship().also { r ->
            r.following = profile.personaConnectivity?.following
                ?: (persona?.following == true)
            r.followed = profile.personaConnectivity?.followed
                ?: (persona?.followed == true)
            r.blocking = profile.isBlocking
            r.muting = profile.isMuted
        }
    }

    /**
     * 検索結果ペルソナ一覧のマッピング
     */
    fun users(
        sources: List<PersonaWithConnectivity>,
        service: Service,
        paging: Paging?,
        nextCursor: String?,
    ): Pageable<User> {
        return Pageable<User>().also { p ->
            p.entities = sources.mapNotNull { source ->
                source.persona?.let { persona ->
                    user(persona, service, source.connectivity)
                }
            }
            p.paging = Mixi2Paging.fromPaging(paging).also { it.nextCursor = nextCursor }
        }
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * コメント (ポスト) のマッピング
     */
    fun comment(
        post: Post,
        users: Map<String, Mixi2User>,
        service: Service,
    ): Mixi2Comment {
        return commentShallow(post, users, service).also { c ->
            post.referencePost?.let { reference ->
                c.sharedComment = commentShallow(reference, users, service)
            }
        }
    }

    /**
     * 参照ポストを再帰せずにマッピングする
     */
    private fun commentShallow(
        post: Post,
        users: Map<String, Mixi2User>,
        service: Service,
    ): Mixi2Comment {
        return Mixi2Comment(service).also { c ->
            c.id = ID(post.postId)
            c.text = post.text?.let { text -> AttributedString.plain(text) }
            c.createAt = instant(post.createdAt)
            c.user = users[post.personaId]
            c.medias = post.medias.map { media(it) }
            c.possiblySensitive = post.isSensitive
            c.liked = post.liked
            c.shared = post.reposted
            c.bookmarked = post.bookmarked
            c.likeCount = post.likesCount.toInt()
            c.shareCount = post.repostCount.toInt()
            c.replyCount = post.repliesCount.toInt()
            c.quoteCount = post.quotedCount.toInt()
            c.replyTo = post.inReplyToPostId?.let { Identify(service, ID(it)) }
            c.communityId = post.community?.communityId
            c.communityName = post.community?.name
            c.reactions = post.stamps.mapNotNull { summary ->
                val stamp = summary.stamp ?: return@mapNotNull null
                Reaction().also { r ->
                    r.name = stamp.stampId
                    r.iconUrl = stamp.url.ifBlank { null }
                    r.count = summary.count.toInt()
                    r.reacting = post.readerStampId == stamp.stampId
                }
            }
        }
    }

    /**
     * タイムラインのマッピング
     */
    fun timeline(
        posts: List<Post>,
        users: Map<String, Mixi2User>,
        service: Service,
        paging: Paging?,
        nextCursor: String?,
    ): Pageable<Comment> {
        return Pageable<Comment>().also { p ->
            p.entities = posts.map { comment(it, users, service) }
            p.paging = Mixi2Paging.fromPaging(paging).also { it.nextCursor = nextCursor }
        }
    }

    /**
     * メディアのマッピング
     */
    fun media(source: Mixi2Media): Media {
        return Media().also { m ->
            val image = source.postImage
            val video = source.postVideo
            val avatar = source.avatar
            when {
                image != null -> {
                    m.type = MediaType.Image
                    m.sourceUrl = image.largeImageUrl.ifBlank { null }
                    m.previewUrl = image.smallImageUrl.ifBlank { null }
                    m.width = image.largeImageWidth
                    m.height = image.largeImageHeight
                    m.blurhash = image.blurhash.ifBlank { null }
                }

                video != null -> {
                    m.type = MediaType.Movie
                    m.sourceUrl = video.url.ifBlank { null }
                    m.previewUrl = video.previewImageUrl.ifBlank { null }
                    m.width = video.width
                    m.height = video.height
                    m.blurhash = video.blurhash.ifBlank { null }
                }

                avatar != null -> {
                    m.type = MediaType.Image
                    m.sourceUrl = avatar.profileImageUrl.ifBlank { avatar.iconUrl }.ifBlank { null }
                    m.previewUrl = avatar.iconUrl.ifBlank { null }
                    m.width = avatar.profileImageWidth
                    m.height = avatar.profileImageHeight
                    m.blurhash = avatar.blurhash.ifBlank { null }
                }

                else -> m.type = MediaType.Other
            }
            m.description = source.description
        }
    }

    // ============================================================== //
    // Community / Chat
    // ============================================================== //
    /**
     * コミュニティ (チャンネル) のマッピング
     */
    fun channel(
        community: Community,
        service: Service,
    ): Mixi2Channel {
        return Mixi2Channel(service).also { c ->
            c.id = ID(community.communityId)
            c.name = community.name
            c.description = community.purpose.ifBlank { null }
            c.createAt = instant(community.createdAt)
            c.isPublic = community.accessLevel == CommunityAccessLevel.PUBLIC
            c.isArchived = community.isArchived
            c.memberCount = community.countOfMembers
                .takeIf { it <= Int.MAX_VALUE }
                ?.toInt()
            c.communityType = community.type.name.lowercase()
            c.accessLevel = community.accessLevel.name.lowercase()
        }
    }

    /**
     * コミュニティ一覧のマッピング
     */
    fun channels(
        communities: List<Community>,
        service: Service,
        paging: Paging?,
        nextCursor: String?,
    ): Pageable<Channel> {
        return Pageable<Channel>().also { p ->
            p.entities = communities.map { channel(it, service) }
            p.paging = Mixi2Paging.fromPaging(paging).also { it.nextCursor = nextCursor }
        }
    }

    /**
     * チャットメッセージ (コメント) のマッピング
     */
    fun chatComment(
        message: ChatRoomMessage,
        users: Map<String, Mixi2User>,
        service: Service,
    ): Mixi2Comment {
        return Mixi2Comment(service).also { c ->
            c.id = ID(message.messageId)
            c.roomId = message.roomId
            c.messageType = message.messageType.name.lowercase()
            c.messageTargetId = message.messageTargetId
            c.text = message.text?.let { text -> AttributedString.plain(text) }
            c.createAt = instant(message.createdAt)
            c.user = users[message.personaId]
            c.medias = message.media.map { media(it) }
            c.directMessage = true
            message.post?.let { post ->
                c.sharedComment = comment(post, users, service)
            }
        }
    }

    /**
     * チャットルーム (スレッド) のマッピング
     */
    fun thread(
        room: ChatRoom,
        users: Map<String, Mixi2User>,
        service: Service,
    ): Mixi2Thread {
        return Mixi2Thread(service).also { t ->
            t.id = ID(room.roomId)
            t.title = room.title
            t.isGroup = room.isGroup
            t.isMuted = room.isMute
            t.isInvisible = room.isInvisible
            t.status = room.status.name.lowercase()
            t.users = room.members.mapNotNull { users[it.personaId] }
            t.lastUpdate = instant(room.message?.createdAt ?: room.createdAt)
            t.description = room.message?.text
        }
    }

    /**
     * チャットルーム一覧のマッピング
     */
    fun threads(
        rooms: List<ChatRoom>,
        users: Map<String, Mixi2User>,
        service: Service,
        paging: Paging?,
        nextCursor: String?,
    ): Pageable<Thread> {
        return Pageable<Thread>().also { p ->
            p.entities = rooms.map { thread(it, users, service) }
            p.paging = Mixi2Paging.fromPaging(paging).also { it.nextCursor = nextCursor }
        }
    }

    // ============================================================== //
    // Notification
    // ============================================================== //
    /**
     * 通知のマッピング
     *
     * The post a notification is about is read back by the action and handed in
     * as [posts]; without it (the post is gone, or was never fetched) the
     * target degrades to a stub carrying the id and web URL only.
     */
    fun notification(
        source: Mixi2RawNotification,
        users: Map<String, Mixi2User>,
        service: Service,
        posts: Map<String, Mixi2Comment> = emptyMap(),
    ): Notification {
        return Mixi2Notification(service).also { n ->
            n.id = ID(source.timeSeriesId)
            n.type = source.activityType.name.lowercase()
            n.action = actionOf(source.activityType)?.code
            n.createAt = instant(source.createdAt)
            n.users = listOfNotNull(users[source.issuerId])
            n.reaction = source.reaction?.stampId?.takeIf { it.isNotBlank() }
            n.iconUrl = source.reaction?.imageUrl?.takeIf { it.isNotBlank() }
            source.postId?.let { postId ->
                n.comments = listOf(
                    posts[postId] ?: Mixi2Comment(service).also { c ->
                        c.id = ID(postId)
                        c.createAt = n.createAt
                        c.webUrl = "${service.host ?: HOST}/posts/$postId"
                    },
                )
            }
        }
    }

    /**
     * 通知一覧のマッピング
     */
    fun notifications(
        sources: List<Mixi2RawNotification>,
        users: Map<String, Mixi2User>,
        service: Service,
        paging: Paging?,
        nextCursor: String?,
        posts: Map<String, Mixi2Comment> = emptyMap(),
    ): Pageable<Notification> {
        return Pageable<Notification>().also { p ->
            p.entities = sources.map { notification(it, users, service, posts) }
            p.paging = Mixi2Paging.fromPaging(paging).also { it.nextCursor = nextCursor }
        }
    }

    /**
     * 通知種別の変換
     */
    fun actionOf(type: NotificationActivityType): NotificationActionType? {
        return when (type) {
            NotificationActivityType.REPLY,
            NotificationActivityType.MENTION,
            -> NotificationActionType.MENTION

            NotificationActivityType.QUOTE -> NotificationActionType.QUOTE

            NotificationActivityType.FOLLOW,
            NotificationActivityType.FOLLOWING_REQUEST_APPROVED,
            -> NotificationActionType.FOLLOW

            NotificationActivityType.FOLLOWING_REQUEST_RECEIVED,
            NotificationActivityType.INVITATION_FOLLOW,
            -> NotificationActionType.FOLLOW_REQUEST

            NotificationActivityType.LIKE -> NotificationActionType.LIKE
            NotificationActivityType.REPOST -> NotificationActionType.SHARE

            NotificationActivityType.REACTION -> NotificationActionType.REACTION

            else -> null
        }
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    /**
     * タイムスタンプの変換
     */
    fun instant(timestamp: Timestamp?): Instant? {
        if (timestamp == null) return null
        return Instant.fromEpochSeconds(timestamp.seconds, timestamp.nanos)
    }
}
