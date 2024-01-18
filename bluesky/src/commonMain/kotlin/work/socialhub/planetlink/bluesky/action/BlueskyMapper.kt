package work.socialhub.planetlink.bluesky.action

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.datetime.toInstant
import work.socialhub.kbsky.model.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.bsky.actor.ActorDefsProfileViewBasic
import work.socialhub.kbsky.model.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.bsky.actor.ActorDefsViewerState
import work.socialhub.kbsky.model.bsky.embed.*
import work.socialhub.kbsky.model.bsky.feed.*
import work.socialhub.kbsky.model.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.kbsky.model.bsky.richtext.RichtextFacetLink
import work.socialhub.kbsky.model.bsky.richtext.RichtextFacetMention
import work.socialhub.planetlink.bluesky.define.BlueskyNotificationType
import work.socialhub.planetlink.bluesky.model.BlueskyChannel
import work.socialhub.planetlink.bluesky.model.BlueskyComment
import work.socialhub.planetlink.bluesky.model.BlueskyPaging
import work.socialhub.planetlink.bluesky.model.BlueskyUser
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.common.AttributedElement
import work.socialhub.planetlink.model.common.AttributedItem
import work.socialhub.planetlink.model.common.AttributedKind
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.utils.CollectionUtil.takeUntil
import kotlin.math.max

object BlueskyMapper {

    // ============================================================== //
    // Single Object Mapper
    // ============================================================== //
    /**
     * (詳細) ユーザーマッピング
     */
    fun user(
        account: ActorDefsProfileViewDetailed,
        service: Service
    ): User {
        return BlueskyUser(service).apply {
            isSimple = false
            protected = false

            id = ID(account.did)
            name = account.displayName
            screenName = account.handle
            iconImageUrl = account.avatar
            coverImageUrl = account.banner

            description = AttributedString.plain(account.description)

            statusesCount = account.postsCount?.toLong()
            followingCount = account.followsCount?.toLong()
            followersCount = account.followersCount?.toLong()

            userViewer(this, account.viewer)
        }
    }

    /**
     * (簡易) ユーザーマッピング
     */
    private fun user(
        account: ActorDefsProfileView,
        service: Service
    ): User {
        return BlueskyUser(service).apply {
            isSimple = true
            protected = false

            id = ID(account.did)
            name = account.displayName
            screenName = account.handle
            iconImageUrl = account.avatar

            description = AttributedString.plain(account.description)

            userViewer(this, account.viewer)
        }
    }

    /**
     * (簡易) ユーザーマッピング
     */
    private fun user(
        account: ActorDefsProfileViewBasic,
        service: Service
    ): User {
        return BlueskyUser(service).apply {
            isSimple = true
            protected = false

            id = ID(account.did)
            name = account.displayName
            screenName = account.handle
            iconImageUrl = account.avatar

            userViewer(this, account.viewer)
        }
    }

    private fun userViewer(
        user: BlueskyUser,
        viewer: ActorDefsViewerState?,
    ) {
        viewer?.let {
            user.followRecordUri = it.following
            user.followedRecordUri = it.followedBy

            user.muted = it.muted
            user.blockedBy = it.blockedBy
            user.blockingRecordUri = it.blocking
        }
    }

    /**
     * コメントマッピング
     */
    fun comment(
        feed: FeedDefsFeedViewPost,
        service: Service
    ): Comment {
        return comment(
            post = feed.post,
            reply = feed.reply,
            repost = feed.reason,
            service = service,
        )
    }

    /**
     * コメントマッピング
     */
    fun comment(
        post: FeedDefsPostView,
        reply: FeedDefsReplyRef?,
        repost: FeedDefsReasonRepost?,
        service: Service
    ): Comment {
        return BlueskyComment(service).apply {

            // Repost
            if (repost != null) {

                id = ID(post.uri!!)
                cid = post.cid
                user = user(repost.by!!, service)
                createAt = repost.indexedAt!!.toInstant()

                medias = mutableListOf()
                sharedComment = comment(
                    post = post,
                    reply = reply,
                    repost = null,
                    service = service,
                )

                liked = false
                shared = false
                possiblySensitive = false

                likeCount = 0L
                shareCount = 0L
                replyCount = 0L
                return@apply
            }

            id = ID(post.uri!!)
            cid = post.cid
            user = user(post.author!!, service)
            createAt = post.indexedAt!!.toInstant()

            // TODO: Labels
            possiblySensitive = false

            likeCount = post.likeCount?.toLong() ?: 0L
            shareCount = post.repostCount?.toLong() ?: 0L
            replyCount = post.replyCount?.toLong() ?: 0L

            commentViewer(this, post.viewer)


            val union = post.record
            if (union is FeedPost) {
                text = attributedText(union)
            }

            // Media + Quote
            medias = mutableListOf()
            post.embed?.let { embed(this, it, service) }

            // Reply
            if (reply != null) {
                // 直接の会話の設定
                replyTo = simpleComment(reply.parent!!, service)
                // 会話スレッドのルート設定
                replyRootTo = simpleComment(reply.root!!, service)
            }
        }
    }

    /**
     * コメントマッピング
     */
    fun simpleComment(
        post: FeedDefsPostView,
        service: Service
    ): Comment {
        return comment(
            post,
            null,
            null,
            service,
        )
    }

    private fun embed(
        model: BlueskyComment,
        embed: EmbedViewUnion,
        service: Service
    ) {
        // Media
        if (embed is EmbedImagesView) {
            val medias = embed.images!!.map { media(it) }
            model.medias = model.medias!!.plus(medias)
        }

        // Quote
        if (embed is EmbedRecordView) {
            embedRecord(model, embed, service)
        }

        // Quote With Media
        if (embed is EmbedRecordWithMediaView) {
            embed(model, embed.media!!, service)
            embedRecord(model, embed.record!!, service)
        }
    }

    private fun embedRecord(
        model: BlueskyComment,
        record: EmbedRecordView,
        service: Service
    ) {
        val union = record.record
        if (union is EmbedRecordViewRecord) {
            model.sharedComment = quote(union, service)
        }
    }

    private fun quote(
        post: EmbedRecordViewRecord,
        service: Service
    ): BlueskyComment {
        return BlueskyComment(service).apply {

            id = ID(post.uri!!)
            cid = post.cid
            user = user(post.author!!, service)
            createAt = post.indexedAt!!.toInstant()

            liked = false
            shared = false
            possiblySensitive = false

            likeCount = 0L
            shareCount = 0L
            replyCount = 0L

            // Text
            val union = post.value
            if (union is FeedPost) {
                text = attributedText(union)
            }

            // Media
            medias = mutableListOf()
            if (post.embeds != null) {
                post.embeds!!.forEach {
                    embed(this, it, service)
                }
            }
        }
    }

    private fun attributedText(
        post: FeedPost
    ): AttributedString {
        val elements = mutableListOf<AttributedElement>()
        var bytes = post.text!!.toByteArray(Charsets.UTF_8)

        // 読み進めたバイト数
        var readIndex = 0

        if (post.facets != null) {
            val facets = post.facets!!.sortedBy { it.index!!.byteStart }

            for (facet in facets) {
                if (facet.features != null && facet.features!!.isNotEmpty()) {
                    val union = facet.features!![0]
                    val index = facet.index!!

                    // Facet の前を Text として取得
                    if (readIndex < index.byteStart!!) {
                        val len: Int = (index.byteStart!! - readIndex)
                        val beforeBytes = bytes.copyOfRange(0, len)

                        // readIndex = index.byteStart!!
                        val afterLen = max(0, (bytes.size - len))
                        bytes = bytes.copyOfRange(len, len + afterLen)

                        val str = String(beforeBytes)
                        val element = AttributedItem()
                        element.kind = AttributedKind.PLAIN
                        element.expandedText = str
                        element.displayText = str
                        elements.add(element)

                        if (bytes.isEmpty()) {
                            break
                        }
                    }

                    // Facet の部分を切り出して作成
                    val len = (index.byteEnd!! - index.byteStart!!)
                    val targetByte = bytes.copyOfRange(0, len)

                    readIndex = index.byteEnd!!
                    val afterLen = max(0, (bytes.size - len))
                    bytes = bytes.copyOfRange(len, len + afterLen)

                    if (union is RichtextFacetMention) {
                        val str = String(targetByte)
                        val element = AttributedItem()
                        element.kind = AttributedKind.ACCOUNT
                        element.expandedText = union.did
                        element.displayText = str
                        elements.add(element)

                    } else if (union is RichtextFacetLink) {
                        val str = String(targetByte)
                        val element = AttributedItem()
                        element.kind = AttributedKind.LINK
                        element.expandedText = union.uri
                        element.displayText = str
                        elements.add(element)
                    } else {
                        // その他の場合はプレーンテキストとして取得
                        val str = String(targetByte)
                        val element = AttributedItem()
                        element.kind = AttributedKind.PLAIN
                        element.expandedText = str
                        element.displayText = str
                        elements.add(element)
                    }

                    if (bytes.isEmpty()) {
                        break
                    }
                }
            }
        }

        if (bytes.isNotEmpty()) {
            val str = String(bytes)
            val element = AttributedItem()
            element.kind = AttributedKind.PLAIN
            element.expandedText = str
            element.displayText = str
            elements.add(element)
        }

        return AttributedString.elements(elements)
    }

    /**
     * メディアマッピング
     */
    private fun media(
        img: EmbedImagesViewImage
    ): Media {
        return Media().apply {
            type = MediaType.Image
            previewUrl = img.thumb
            sourceUrl = img.fullsize
        }
    }

    private fun commentViewer(
        comment: BlueskyComment,
        viewer: FeedDefsViewerState?,
    ) {
        viewer?.let {
            comment.liked = it.like != null
            comment.likeRecordUri = it.like
            comment.shared = it.repost != null
            comment.repostRecordUri = it.repost
        }
    }

    /**
     * ユーザー関係マッピング
     */
    fun relationship(
        user: BlueskyUser
    ): Relationship {
        return Relationship().apply {
            following = user.followRecordUri != null
            followed = user.followedRecordUri != null

            muting = user.muted ?: false
            blocking = user.blockingRecordUri != null
        }
    }

    /**
     * 通知マッピング
     */
    fun notification(
        notification: NotificationListNotificationsNotification,
        posts: Map<String, FeedDefsPostView>,
        service: Service
    ): Notification {
        return Notification(service).apply {
            id = ID(notification.uri)
            createAt = notification.indexedAt.toInstant()

            val type = BlueskyNotificationType.of(notification.reason)
            if (type != null) {
                this.type = type.code
                this.action = type.action.code
            }

            users = mutableListOf<User>().apply {
                add(user(notification.author, service))
            }

            val union = notification.record
            if (union is FeedLike || union is FeedRepost) {
                val subject = notification.reasonSubject
                val post = posts[subject]

                if (post != null) {
                    comments = mutableListOf<Comment>().apply {
                        add(simpleComment(post, service))
                    }
                }
            }
        }
    }

    /**
     * チャンネルマッピング
     */
    fun channel(
        generator: FeedDefsGeneratorView,
        service: Service
    ): Channel {
        return BlueskyChannel(service).apply {
            id = ID(generator.uri!!)
            cid = generator.cid
            public = true

            name = generator.displayName
            description = generator.description
            createAt = generator.indexedAt!!.toInstant()

            owner = user(generator.creator!!, service)
            likeCount = generator.likeCount ?: 0
            iconUrl = generator.avatar
        }
    }

    // ============================================================== //
    // List Object Mapper
    // ============================================================== //
    /**
     * ユーザーマッピング
     */
    fun users(
        users: List<ActorDefsProfileView>,
        cursor: String?,
        paging: Paging,
        service: Service
    ): Pageable<User> {

        var userList = users
        if (paging is BlueskyPaging) {
            userList = userList.takeUntil {
                val id = paging.latestRecord
                id != null && it.did == id.id!!.value<String>()
            }
        }

        // 空の場合
        if (userList.isEmpty()) {
            val model = Pageable<User>()
            model.paging = paging
            return model
        }

        val model = Pageable<User>()
        model.entities = userList.map { user(it, service) }
        model.paging = BlueskyPaging.fromPaging(paging)
            .also { it.cursorHint = cursor }
        return model
    }

    /**
     * タイムラインマッピング
     */
    fun timelineByFeeds(
        feed: List<FeedDefsFeedViewPost>,
        paging: Paging,
        service: Service,
    ): Pageable<Comment> {

        var feedList = feed
        if (paging is BlueskyPaging) {
            feedList = feedList.takeUntil {
                val id = paging.latestRecord
                id != null && it.post.uri == id.id!!.value<String>()
            }
        }

        // 空の場合
        if (feedList.isEmpty()) {
            val model = Pageable<Comment>()
            model.paging = paging
            return model
        }

        val model = Pageable<Comment>()
        model.entities = feedList.map { comment(it, service) }
        model.paging = BlueskyPaging.fromPaging(paging)
        return model
    }

    /**
     * タイムラインマッピング
     */
    fun timelineByPosts(
        posts: List<FeedDefsPostView>,
        paging: Paging?,
        service: Service
    ): Pageable<Comment> {

        var postList = posts
        if (paging is BlueskyPaging) {
            postList = postList.takeUntil {
                val id = paging.latestRecord
                id != null && it.uri == id.id!!.value<String>()
            }
        }

        // 空の場合
        if (postList.isEmpty()) {
            val model = Pageable<Comment>()
            model.paging = paging
            return model
        }

        val model = Pageable<Comment>()
        model.entities = postList.map { simpleComment(it, service) }
        model.paging = BlueskyPaging.fromPaging(paging)
        return model
    }

    /**
     * 通知マッピング
     */
    fun notifications(
        notifications: List<NotificationListNotificationsNotification>,
        posts: List<FeedDefsPostView>,
        paging: Paging?,
        service: Service
    ): Pageable<Notification> {

        var notificationList = notifications
        if (paging is BlueskyPaging) {
            notificationList = notificationList.takeUntil {
                val id = paging.latestRecord
                id != null && it.uri == id.id!!.value<String>()
            }
        }

        // 空の場合
        if (notificationList.isEmpty()) {
            val model = Pageable<Notification>()
            model.paging = paging
            return model
        }

        val postMap = mutableMapOf<String, FeedDefsPostView>()
        posts.forEach { postMap[it.uri!!] = it }

        val model = Pageable<Notification>()
        model.entities = notificationList.map { notification(it, postMap, service) }
        model.paging = BlueskyPaging.fromPaging(paging)
        return model
    }

    /**
     * チャンネル一覧マッピング
     */
    fun channels(
        channels: List<FeedDefsGeneratorView>,
        paging: Paging,
        service: Service
    ): Pageable<Channel> {

        var channelList = channels
        if (paging is BlueskyPaging) {
            channelList = channelList.takeUntil {
                val id = paging.latestRecord
                id != null && it.uri == id.id!!.value<String>()
            }
        }

        // 空の場合
        if (channelList.isEmpty()) {
            val model = Pageable<Channel>()
            model.paging = paging
            return model
        }

        val model = Pageable<Channel>()
        model.entities = channelList.map { channel(it, service) }
        model.paging = BlueskyPaging.fromPaging(paging)
        return model
    }
}
