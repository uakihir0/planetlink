package work.socialhub.planetlink.bluesky.action

import kotlinx.datetime.Instant
import work.socialhub.kbsky.internal.share._InternalUtility
import work.socialhub.kbsky.model.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.planetlink.bluesky.model.BlueskyUser
import work.socialhub.planetlink.model.*
import kotlin.math.max
import kotlin.reflect.KClass

object BlueskyMapper {

    private const val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    /** ダイナミックロードできない為に使用を明示するために使用  */
    private val ClassLoader: List<KClass<*>> = listOf()

    /** 時間のパーサーオブジェクト  */
    private var dateParser: SimpleDateFormat? = null

    // ============================================================== //
    // Single Object Mapper
    // ============================================================== //
    /**
     * (詳細) ユーザーマッピング
     */
    fun user(
        account: ActorDefsProfileViewDetailed,
        service: Service?
    ): User {
        val user = BlueskyUser(service)
        user.setSimple(false)

        user.setId(account.getDid())
        user.setScreenName(account.getHandle())

        user.setName(account.getDisplayName())
        user.setIconImageUrl(account.getAvatar())
        user.setCoverImageUrl(account.getBanner())

        if (account.getPostsCount() != null) {
            user.setStatusesCount(account.getPostsCount().longValue())
        }
        if (account.getFollowersCount() != null) {
            user.setFollowersCount(account.getFollowersCount().longValue())
        }
        if (account.getFollowsCount() != null) {
            user.setFollowingsCount(account.getFollowsCount().longValue())
        }

        if (account.getViewer() != null) {
            val state: ActorDefsViewerState = account.getViewer()
            user.setFollowRecordUri(state.getFollowing())
            user.setFollowedRecordUri(state.getFollowedBy())
            user.setMuted(state.getMuted())
            user.setBlockedBy(state.getBlockedBy())
            user.setBlockingRecordUri(state.getBlocking())
        }

        user.setDescription(AttributedString.plain(account.getDescription()))
        user.setProtected(false)

        return user
    }

    /**
     * (簡易) ユーザーマッピング
     */
    private fun user(
        account: ActorDefsProfileView,
        service: Service
    ): User {
        val user: BlueskyUser = BlueskyUser(service)
        user.setSimple(true)

        user.setId(account.getDid())
        user.setScreenName(account.getHandle())

        user.setName(account.getDisplayName())
        user.setIconImageUrl(account.getAvatar())

        if (account.getViewer() != null) {
            val state: ActorDefsViewerState = account.getViewer()
            user.setFollowRecordUri(state.getFollowing())
            user.setFollowedRecordUri(state.getFollowedBy())
            user.setMuted(state.getMuted())
            user.setBlockedBy(state.getBlockedBy())
            user.setBlockingRecordUri(state.getBlocking())
        }

        user.setDescription(AttributedString.plain(account.getDescription()))
        user.setProtected(false)
        return user
    }

    /**
     * (簡易) ユーザーマッピング
     */
    private fun user(
        account: ActorDefsProfileViewBasic,
        service: Service
    ): User {
        val user: BlueskyUser = BlueskyUser(service)
        user.setSimple(true)

        user.setId(account.getDid())
        user.setScreenName(account.getHandle())

        user.setName(account.getDisplayName())
        user.setIconImageUrl(account.getAvatar())

        if (account.getViewer() != null) {
            val state: ActorDefsViewerState = account.getViewer()
            user.setFollowRecordUri(state.getFollowing())
            user.setFollowedRecordUri(state.getFollowedBy())
            user.setMuted(state.getMuted())
            user.setBlockedBy(state.getBlockedBy())
            user.setBlockingRecordUri(state.getBlocking())
        }

        user.setProtected(false)
        return user
    }

    /**
     * コメントマッピング
     */
    fun comment(
        feed: FeedDefsFeedViewPost,
        service: Service?
    ): Comment {
        val post: FeedDefsPostView = feed.getPost()
        val reply: FeedDefsReplyRef = feed.getReply()
        val repost: FeedDefsReasonRepost = feed.getReason()
        return comment(post, reply, repost, service)
    }

    /**
     * コメントマッピング
     */
    fun comment(
        post: FeedDefsPostView,
        reply: FeedDefsReplyRef?,
        repost: FeedDefsReasonRepost?,
        service: Service?
    ): Comment {
        val model: BlueskyComment = BlueskyComment(service)

        // Repost
        if (repost != null) {
            model.setId(post.getUri())
            model.setCid(post.getCid())
            model.setUser(user(repost.getBy(), service))
            model.setCreateAt(parseDate(repost.getIndexedAt()))

            model.setMedias(java.util.ArrayList<E>())
            model.setSharedComment(
                comment(
                    post, reply, null, service
                )
            )

            model.setLiked(false)
            model.setShared(false)
            model.setPossiblySensitive(false)

            model.setLikeCount(0L)
            model.setShareCount(0L)
            model.setReplyCount(0L)

            return model
        }


        model.setId(post.getUri())
        model.setCid(post.getCid())
        model.setUser(user(post.getAuthor(), service))
        model.setCreateAt(parseDate(post.getIndexedAt()))

        // TODO: Labels
        model.setPossiblySensitive(false)

        model.setLikeCount(
            if ((post.getLikeCount() != null)
            ) post.getLikeCount().longValue() else 0
        )
        model.setShareCount(
            if ((post.getRepostCount() != null)
            ) post.getRepostCount().longValue() else 0
        )
        model.setReplyCount(
            if ((post.getReplyCount() != null)
            ) post.getReplyCount().longValue() else 0
        )

        val state: FeedDefsViewerState = post.getViewer()
        model.setLiked(state.getLike() != null)
        model.setLikeRecordUri(state.getLike())
        model.setShared(state.getRepost() != null)
        model.setRepostRecordUri(state.getRepost())

        val union: RecordUnion = post.getRecord()
        if (union is FeedPost) {
            val record: FeedPost = union as FeedPost
            model.setText(getAttributedText(record))
        }

        // Media + Quote
        val embed: EmbedViewUnion = post.getEmbed()
        model.setMedias(java.util.ArrayList<E>())
        embed(model, embed, service)

        // Reply
        if (reply != null) {
            // リプライ設定

            val parent: FeedDefsPostView = reply.getParent()
            val parentComment: BlueskyComment = simpleComment(parent, service) as BlueskyComment
            model.setReplyTo(parentComment)

            // 会話スレッドのルート設定
            val root: FeedDefsPostView = reply.getRoot()
            val rootComment: BlueskyComment = simpleComment(root, service) as BlueskyComment
            model.setReplayRootTo(rootComment)
        }

        return model
    }

    /**
     * コメントマッピング
     */
    fun simpleComment(
        post: FeedDefsPostView,
        service: Service?
    ): Comment {
        return comment(
            post,
            null,
            null,
            service
        )
    }

    private fun embed(
        model: BlueskyComment,
        embed: EmbedViewUnion,
        service: Service?
    ) {
        // Media
        if (embed is EmbedImagesView) {
            model.getMedias().clear()
            (embed as EmbedImagesView).getImages().forEach { img -> model.getMedias().add(media(img)) }
        }

        // Quote
        if (embed is EmbedRecordView) {
            embedRecord(model, embed as EmbedRecordView, service)
        }

        // Quote With Media
        if (embed is EmbedRecordWithMediaView) {
            embed(model, (embed as EmbedRecordWithMediaView).getMedia(), service)
            embedRecord(model, (embed as EmbedRecordWithMediaView).getRecord(), service)
        }
    }

    private fun embedRecord(
        model: BlueskyComment,
        record: EmbedRecordView,
        service: Service?
    ) {
        val union: EmbedRecordViewUnion = record.getRecord()
        if (union is EmbedRecordViewRecord) {
            val v: EmbedRecordViewRecord = union as EmbedRecordViewRecord
            model.setSharedComment(quote(v, service))
        }

        if (union is EmbedRecordWithMediaView) {
            val mv: EmbedRecordWithMediaView = union as EmbedRecordWithMediaView
            embedRecord(model, mv.getRecord(), service)
            embed(model, mv.getMedia(), service)
        }
    }

    private fun quote(
        post: EmbedRecordViewRecord,
        service: Service?
    ): BlueskyComment {
        val model: BlueskyComment = BlueskyComment(service)

        model.setId(post.getUri())
        model.setCid(post.getCid())
        model.setUser(user(post.getAuthor(), service))
        model.setCreateAt(parseDate(post.getIndexedAt()))

        model.setLiked(false)
        model.setShared(false)
        model.setPossiblySensitive(false)

        model.setLikeCount(0L)
        model.setShareCount(0L)
        model.setReplyCount(0L)

        // Text
        val union: RecordUnion = post.getValue()
        if (union is FeedPost) {
            val record: FeedPost = union as FeedPost
            model.setText(getAttributedText(record))
        }

        // Media
        model.setMedias(java.util.ArrayList<E>())
        if (post.getEmbeds() != null) {
            post.getEmbeds().forEach { embed -> embed(model, embed, service) }
        }

        return model
    }

    private fun getAttributedText(post: FeedPost): AttributedString {
        val elements: MutableList<AttributedElement> = java.util.ArrayList<AttributedElement>()
        var bytes: ByteArray = post.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8)

        // 読み進めたバイト数
        var readIndex = 0

        if (post.getFacets() != null) {
            val facets: List<RichtextFacet> = post.getFacets().stream()
                .sorted(java.util.Comparator.comparing(java.util.function.Function<T, U> { i: T ->
                    i.getIndex().getByteStart()
                }))
                .collect(java.util.stream.Collectors.toList())

            for (facet in facets) {
                if (facet.getFeatures() != null && !facet.getFeatures().isEmpty()) {
                    val union: RichtextFacetFeatureUnion = facet.getFeatures().get(0)
                    val index: RichtextFacetByteSlice = facet.getIndex()

                    // 処理しない Union の場合は次
                    if (union == null) {
                        continue
                    }

                    // Facet の前を Text として取得
                    if (readIndex < index.getByteStart()) {
                        val len: Int = (index.getByteStart() - readIndex)
                        val beforeBytes: ByteArray = java.util.Arrays.copyOfRange(bytes, 0, len)

                        readIndex = index.getByteStart()
                        val afterLen = max(0.0, (bytes.size - len).toDouble()).toInt()
                        bytes = java.util.Arrays.copyOfRange(bytes, len, len + afterLen)

                        val str: String = String(beforeBytes, java.nio.charset.StandardCharsets.UTF_8)
                        val element: AttributedItem = AttributedItem()
                        element.setKind(AttributedKind.PLAIN)
                        element.setExpandedText(str)
                        element.setDisplayText(str)
                        elements.add(element)

                        if (bytes.size == 0) {
                            break
                        }
                    }

                    // Facet の部分を切り出して作成
                    val len: Int = (index.getByteEnd() - index.getByteStart())
                    val targetByte: ByteArray = java.util.Arrays.copyOfRange(bytes, 0, len)

                    readIndex = index.getByteEnd()
                    val afterLen = max(0.0, (bytes.size - len).toDouble()).toInt()
                    bytes = java.util.Arrays.copyOfRange(bytes, len, len + afterLen)

                    if (union is RichtextFacetMention) {
                        val mention: RichtextFacetMention = union as RichtextFacetMention
                        val str: String = String(targetByte, java.nio.charset.StandardCharsets.UTF_8)
                        val element: AttributedItem = AttributedItem()
                        element.setKind(AttributedKind.ACCOUNT)
                        element.setExpandedText(mention.getDid())
                        element.setDisplayText(str)
                        elements.add(element)
                    } else if (union is RichtextFacetLink) {
                        val link: RichtextFacetLink = union as RichtextFacetLink
                        val str: String = String(targetByte, java.nio.charset.StandardCharsets.UTF_8)
                        val element: AttributedItem = AttributedItem()
                        element.setKind(AttributedKind.LINK)
                        element.setExpandedText(link.getUri())
                        element.setDisplayText(str)
                        elements.add(element)
                    } else {
                        // その他の場合はプレーンテキストとして取得
                        val str: String = String(targetByte, java.nio.charset.StandardCharsets.UTF_8)
                        val element: AttributedItem = AttributedItem()
                        element.setKind(AttributedKind.PLAIN)
                        element.setExpandedText(str)
                        element.setDisplayText(str)
                        elements.add(element)
                    }

                    if (bytes.size == 0) {
                        break
                    }
                }
            }
        }

        if (bytes.size > 0) {
            val str: String = String(bytes, java.nio.charset.StandardCharsets.UTF_8)
            val element: AttributedItem = AttributedItem()
            element.setKind(AttributedKind.PLAIN)
            element.setExpandedText(str)
            element.setDisplayText(str)
            elements.add(element)
        }

        return AttributedString.elements(elements)
    }

    /**
     * メディアマッピング
     */
    private fun media(img: EmbedImagesViewImage): Media {
        val media: Media = Media()
        media.setType(MediaType.Image)
        media.setPreviewUrl(img.getThumb())
        media.setSourceUrl(img.getFullsize())
        return media
    }

    /**
     * ユーザー関係マッピング
     */
    fun relationship(
        user: BlueskyUser
    ): Relationship {
        val relationship: Relationship = Relationship()
        relationship.setFollowing(user.getFollowRecordUri() != null)
        relationship.setFollowed(user.getFollowedRecordUri() != null)
        relationship.setMuting(if ((user.getMuted() != null)) user.getMuted() else false)
        relationship.setBlocking((user.getBlockingRecordUri() != null))
        return relationship
    }

    /**
     * 通知マッピング
     */
    fun notification(
        notification: NotificationListNotificationsNotification,
        posts: Map<String?, FeedDefsPostView?>,
        service: Service?
    ): Notification {
        val model: Notification = Notification(service)
        model.setId(notification.getUri())
        model.setCreateAt(parseDate(notification.getIndexedAt()))

        val type: BlueskyNotificationType =
            BlueskyNotificationType
                .of(notification.getReason())

        if (type != null) {
            model.setType(type.getCode())
            if (type.getAction() != null) {
                model.setAction(type.getAction().getCode())
            }
        }

        if (notification.getAuthor() != null) {
            model.setUsers(java.util.ArrayList<E>())
            model.getUsers().add(user(notification.getAuthor(), service))
        }


        if (notification.getRecord() != null) {
            val union: RecordUnion = notification.getRecord()

            if (union is FeedLike ||
                union is FeedRepost
            ) {
                val subject: String = notification.getReasonSubject()
                val post: FeedDefsPostView? = posts[subject]

                if (post != null) {
                    model.setComments(java.util.ArrayList<E>())
                    model.getComments().add(simpleComment(post, service))
                }
            }
        }

        return model
    }

    /**
     * チャンネルマッピング
     */
    fun channel(
        generator: FeedDefsGeneratorView,
        service: Service?
    ): Channel {
        val model: BlueskyChannel = BlueskyChannel(service)

        model.setId(generator.getUri())
        model.setCid(generator.getCid())
        model.setPublic(true)

        model.setName(generator.getDisplayName())
        model.setDescription(generator.getDescription())
        model.setCreateAt(parseDate(generator.getIndexedAt()))

        model.setOwner(user(generator.getCreator(), service))
        model.setLikeCount(generator.getLikeCount())
        model.setIconUrl(generator.getAvatar())

        return model
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
        service: Service?
    ): Pageable<User> {
        var users: List<ActorDefsProfileView> = users
        if (paging is BlueskyPaging) {
            val p: BlueskyPaging = paging as BlueskyPaging
            users = takeUntil(users, java.util.function.Predicate<T> { f: T ->
                val id: Identify = p.getLatestRecord()
                id != null && f.getDid().equals(id.getId())
            })
        }

        // 空の場合
        if (users.isEmpty()) {
            val model: Pageable<User> = Pageable()
            model.setEntities(java.util.ArrayList<E>())
            model.setPaging(paging)
            return model
        }

        val model: Pageable<User> = Pageable()
        model.setEntities(
            users.stream()
                .map<Any>(java.util.function.Function<ActorDefsProfileView, Any> { a: ActorDefsProfileView? ->
                    user(
                        a,
                        service
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        val pg: BlueskyPaging = BlueskyPaging.fromPaging(paging)
        pg.setCursorHint(cursor)
        model.setPaging(pg)
        return model
    }

    /**
     * タイムラインマッピング
     */
    fun timelineByFeeds(
        feed: List<FeedDefsFeedViewPost>,
        paging: Paging,
        service: Service?
    ): Pageable<Comment> {
        var feed: List<FeedDefsFeedViewPost> = feed
        if (paging is BlueskyPaging) {
            val p: BlueskyPaging = paging as BlueskyPaging
            feed = takeUntil(feed, java.util.function.Predicate<T> { f: T ->
                val id: Identify = p.getLatestRecord()
                id != null && f.getPost()
                    .getUri().equals(id.getId())
            })
        }

        // 空の場合
        if (feed.isEmpty()) {
            val model: Pageable<Comment> = Pageable()
            model.setEntities(java.util.ArrayList<E>())
            model.setPaging(paging)
            return model
        }

        val model: Pageable<Comment> = Pageable()
        model.setEntities(
            feed.stream()
                .map<Any>(java.util.function.Function<FeedDefsFeedViewPost, Any> { a: FeedDefsFeedViewPost ->
                    comment(
                        a,
                        service
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(BlueskyPaging.fromPaging(paging))
        return model
    }

    /**
     * タイムラインマッピング
     */
    fun timelineByPosts(
        posts: List<FeedDefsPostView>,
        paging: Paging?,
        service: Service?
    ): Pageable<Comment> {
        var posts: List<FeedDefsPostView> = posts
        if (paging is BlueskyPaging) {
            val p: BlueskyPaging? = paging as BlueskyPaging?
            posts = takeUntil(posts, java.util.function.Predicate<T> { f: T ->
                val id: Identify = p.getLatestRecord()
                id != null && f.getUri().equals(id.getId())
            })
        }

        // 空の場合
        if (posts.isEmpty()) {
            val model: Pageable<Comment> = Pageable()
            model.setEntities(java.util.ArrayList<E>())
            model.setPaging(paging)
            return model
        }

        val model: Pageable<Comment> = Pageable()
        model.setEntities(
            posts.stream()
                .map<Any>(java.util.function.Function<FeedDefsPostView, Any> { a: FeedDefsPostView ->
                    simpleComment(
                        a,
                        service
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(BlueskyPaging.fromPaging(paging))
        return model
    }

    /**
     * 通知マッピング
     */
    fun notifications(
        notifications: List<NotificationListNotificationsNotification>?,
        posts: List<FeedDefsPostView>,
        paging: Paging?,
        service: Service?
    ): Pageable<Notification> {
        var notifications: List<NotificationListNotificationsNotification>? = notifications
        if (paging is BlueskyPaging) {
            val p: BlueskyPaging? = paging as BlueskyPaging?
            notifications = takeUntil(notifications, java.util.function.Predicate<T> { f: T ->
                val id: Identify = p.getLatestRecord()
                id != null && f.getUri().equals(id.getId())
            })
        }

        // 空の場合
        if (notifications!!.isEmpty()) {
            val model: Pageable<Notification> = Pageable()
            model.setEntities(java.util.ArrayList<E>())
            model.setPaging(paging)
            return model
        }

        val postMap: MutableMap<String?, FeedDefsPostView?> = java.util.HashMap<String, FeedDefsPostView>()
        posts.forEach(java.util.function.Consumer<FeedDefsPostView> { a: FeedDefsPostView ->
            postMap.put(
                a.getUri(),
                a
            )
        })

        val model: Pageable<Notification> = Pageable()
        model.setEntities(
            notifications.stream()
                .map<Any>(java.util.function.Function<NotificationListNotificationsNotification, Any> { n: NotificationListNotificationsNotification ->
                    notification(
                        n,
                        postMap,
                        service
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(BlueskyPaging.fromPaging(paging))
        return model
    }

    /**
     * チャンネル一覧マッピング
     */
    fun channels(
        channels: List<FeedDefsGeneratorView>,
        paging: Paging,
        service: Service?
    ): Pageable<Channel> {
        var channels: List<FeedDefsGeneratorView> = channels
        if (paging is BlueskyPaging) {
            val p: BlueskyPaging = paging as BlueskyPaging
            channels = takeUntil(channels, java.util.function.Predicate<T> { c: T ->
                val id: Identify = p.getLatestRecord()
                id != null && c.getUri().equals(id.getId())
            })
        }

        // 空の場合
        if (channels.isEmpty()) {
            val model: Pageable<Channel> = Pageable()
            model.setEntities(java.util.ArrayList<E>())
            model.setPaging(paging)
            return model
        }

        val model: Pageable<Channel> = Pageable()
        model.setEntities(
            channels.stream()
                .map<Any>(java.util.function.Function<FeedDefsGeneratorView, Any> { c: FeedDefsGeneratorView ->
                    channel(
                        c,
                        service
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(BlueskyPaging.fromPaging(paging))
        return model
    }

    fun <T> takeUntil(
        list: List<T>,
        predicate: (T) -> Boolean
    ): List<T> {
        val result = mutableListOf<T>()
        for (item in list) {
            if (predicate(item)) {
                break
            }
            result.add(item)
        }
        return result
    }

    fun formatDate(date: Instant): String {
        if (dateParser == null) {
            dateParser = SimpleDateFormat(dateFormat)
            dateParser.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
        }
        return dateParser.format(date)
    }
}
