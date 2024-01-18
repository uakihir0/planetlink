package work.socialhub.planetlink.bluesky.action

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.toInstant
import work.socialhub.kbsky.BlueskyException
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.api.entity.atproto.identity.IdentityResolveHandleRequest
import work.socialhub.kbsky.api.entity.atproto.repo.RepoListRecordsRequest
import work.socialhub.kbsky.api.entity.atproto.repo.RepoUploadBlobRequest
import work.socialhub.kbsky.api.entity.atproto.server.ServerCreateSessionRequest
import work.socialhub.kbsky.api.entity.bsky.actor.ActorGetPreferencesRequest
import work.socialhub.kbsky.api.entity.bsky.actor.ActorGetProfileRequest
import work.socialhub.kbsky.api.entity.bsky.actor.ActorSearchActorsRequest
import work.socialhub.kbsky.api.entity.bsky.feed.*
import work.socialhub.kbsky.api.entity.bsky.graph.*
import work.socialhub.kbsky.api.entity.bsky.notification.NotificationListNotificationsRequest
import work.socialhub.kbsky.api.entity.bsky.notification.NotificationUpdateSeenRequest
import work.socialhub.kbsky.model.atproto.repo.RepoStrongRef
import work.socialhub.kbsky.model.bsky.actor.ActorDefsSavedFeedsPref
import work.socialhub.kbsky.model.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.bsky.embed.EmbedImagesImage
import work.socialhub.kbsky.model.bsky.embed.EmbedRecord
import work.socialhub.kbsky.model.bsky.embed.EmbedRecordWithMedia
import work.socialhub.kbsky.model.bsky.feed.*
import work.socialhub.kbsky.model.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.kbsky.model.bsky.richtext.RichtextFacet
import work.socialhub.kbsky.util.ATUriParser
import work.socialhub.kbsky.util.facet.FacetType
import work.socialhub.kbsky.util.facet.FacetUtil
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.bluesky.define.BlueskyReactionType
import work.socialhub.planetlink.bluesky.model.BlueskyComment
import work.socialhub.planetlink.bluesky.model.BlueskyPaging
import work.socialhub.planetlink.bluesky.model.BlueskyUser
import work.socialhub.planetlink.bluesky.support.Utils
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.utils.CollectionUtil.takeUntil
import kotlin.math.min
import work.socialhub.planetlink.bluesky.action.BlueskyMapper as Mapper

class BlueskyAction(
    account: Account,
    var auth: BlueskyAuth,
) : AccountActionImpl(account) {

    private var accessJwt: String? = null
    private var expireAt: Long? = null
    private var did: String? = null

    // ============================================================== //
    // Account
    // ============================================================== //

    /**
     * {@inheritDoc}
     */
    override fun userMe(): User {
        return proceed {
            val response = auth.accessor.actor().getProfile(
                ActorGetProfileRequest(accessJwt())
                    .also { it.actor = did() }
            )

            Mapper.user(response.data, service())
                .also { this.me = it }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun user(
        id: Identify
    ): User {
        return proceed {
            val response = auth.accessor.actor().getProfile(
                ActorGetProfileRequest(accessJwt())
                    .also { it.actor = id.id!!.value() }
            )
            Mapper.user(response.data, service())
        }
    }

    /**
     * {@inheritDoc}
     * ハンドルも DID も同じ位置で解釈される
     * https://staging.bsky.app/profile/uakihir0.com
     * https://staging.bsky.app/profile/did:plc:bwdof2anluuf5wmfy2upgulw
     */
    override fun user(
        url: String
    ): User {
        return proceed {
            val id = Utils.userIdentifyFromUrl(url)
            val response = auth.accessor.actor().getProfile(
                ActorGetProfileRequest(accessJwt())
                    .also { it.actor = id }
            )
            Mapper.user(response.data, service())
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun followUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().follow(
                GraphFollowRequest(accessJwt())
                    .also { it.subject = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unfollowUser(
        id: Identify
    ) {
        proceedUnit {
            // TODO: uri の取得について確認
            userUri(id).let { uri ->
                auth.accessor.graph().deleteFollow(
                    GraphDeleteFollowRequest(accessJwt())
                        .also { it.uri = uri }
                )
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun muteUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().muteActor(
                GraphMuteActorRequest(accessJwt())
                    .also { it.actor = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unmuteUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().unmuteActor(
                GraphUnmuteActorRequest(accessJwt())
                    .also { it.actor = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun blockUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().block(
                GraphBlockRequest(accessJwt())
                    .also { it.subject = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unblockUser(
        id: Identify
    ) {
        proceedUnit {
            // TODO: uri の取得について確認
            userUri(id).let { uri ->
                auth.accessor.graph().deleteBlock(
                    GraphDeleteBlockRequest(accessJwt())
                        .also { it.uri = uri }
                )
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun relationship(
        id: Identify
    ): Relationship {
        return proceed {
            if (id is BlueskyUser) {
                Mapper.relationship(id)
            } else {
                val user = user(id) as BlueskyUser
                Mapper.relationship(user)
            }
        }
    }

    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun followingUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val follows =
                auth.accessor.graph().getFollows(
                    GraphGetFollowsRequest(accessJwt()).also {
                        it.actor = id.id!!.value()
                        it.cursor = cursor(paging)
                        it.limit = limit(paging)
                    }
                )

            Mapper.users(
                follows.data.follows,
                follows.data.cursor,
                paging,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun followerUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val follows =
                auth.accessor.graph().getFollowers(
                    GraphGetFollowersRequest(accessJwt()).also {
                        it.actor = id.id!!.value()
                        it.cursor = cursor(paging)
                        it.limit = limit(paging)
                    }
                )
            Mapper.users(
                follows.data.followers,
                follows.data.cursor,
                paging,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun searchUsers(
        query: String,
        paging: Paging
    ): Pageable<User> {
        return proceed {
            val response =
                auth.accessor.actor().searchActors(
                    ActorSearchActorsRequest(accessJwt()).also {
                        it.cursor = cursor(paging)
                        it.limit = limit(paging)
                        it.q = query
                    }
                )
            Mapper.users(
                response.data.actors,
                response.data.cursor,
                paging,
                service(),
            )
        }
    }

    // ============================================================== //
    // Timeline
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun homeTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val response = auth.accessor.feed().getTimeline(
                FeedGetTimelineRequest(accessJwt()).also {
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )

            Mapper.timelineByFeeds(
                response.data.feed,
                paging,
                service()
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun mentionTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {

            // 取得する通知の種類を指定
            val types = listOf("reply", "quote")

            val pg = countLimitPaging(paging, 20)
            val model = notifications(pg, types)

            // 空の場合
            if (model.notifications!!.isEmpty()) {
                val results = Pageable<Comment>()
                results.entities = listOf()
                results.paging = paging
                return@proceed results
            }

            // 投稿を取得
            val subjects = model.notifications!!
                .map { it.uri }

            val results = Mapper.timelineByPosts(
                postViews(subjects),
                null,
                service(),
            )

            // ページング情報を上書きする (ヒントの追加)
            val bp = BlueskyPaging.fromPaging(paging)
            val id = Identify(service())
            id.id = ID(model.first!!)

            bp.cursorHint = model.cursor
            bp.latestRecordHint = id
            results.paging = bp
            results
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userCommentTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response =
                auth.accessor.feed().getAuthorFeed(
                    FeedGetAuthorFeedRequest(accessJwt()).also {
                        it.actor = id.id!!.value()
                        it.cursor = cursor(paging)
                        it.limit = limit(paging)
                    }
                )
            Mapper.timelineByFeeds(
                response.data.feed,
                paging,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userLikeTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response =
                auth.accessor.repo().listRecords(
                    RepoListRecordsRequest(
                        repo = id.id!!.value(),
                        collection = BlueskyTypes.FeedLike,
                    ).also {
                        it.cursor = rkey(cursor(paging))
                        it.limit = limit(paging)
                    }
                )

            var records = response.data.records

            // 取得済みレコードを結果から排除
            if (paging is BlueskyPaging) {

                if (paging.latestRecord != null) {
                    val uri = paging.latestRecord!!.id!!.value as String
                    records = records.takeUntil { it.uri == uri }
                }
            }

            // Like した投稿のレコード uri を取得
            val subjects = records
                .filter { it.value is FeedLike }
                .map { it.value as FeedLike }
                .map { it.subject!!.uri }

            // 空の場合
            if (subjects.isEmpty()) {
                val results = Pageable<Comment>()
                results.paging = paging
                return@proceed results
            }

            val results = Mapper.timelineByPosts(
                postViews(subjects),
                null,
                service(),
            )

            // ページングを上書きする
            val pg = BlueskyPaging.fromPaging(paging)
            pg.cursorHint = records[records.size - 1].uri
            val lid = Identify(service(), ID(records[0].uri!!))
            pg.latestRecordHint = lid
            results.paging = pg
            results
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userMediaTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {

            // 画像の取得数は 20 件に制限
            val limit = min(limit(paging), 20)
            var cursor = cursor(paging)

            val feeds = mutableListOf<FeedDefsFeedViewPost>()

            // 十分な数の投稿が取得できるまでリクエストを実行
            for (i in 0..9) {
                val response =
                    auth.accessor.feed().getAuthorFeed(
                        FeedGetAuthorFeedRequest(accessJwt()).also {
                            it.actor = id.id!!.value()
                            it.cursor = cursor
                            it.limit = limit
                        }
                    )

                // 画像の投稿が含まれているものだけを抽出
                val imagePosts =
                    response.data.feed.filter { f ->
                        val union = f.post.record
                        if (union is FeedPost) {
                            val embed = union.embed
                            if (embed is EmbedImages) {
                                return@filter embed.images!!.isNotEmpty()
                            }
                        }
                        false
                    }

                feeds.addAll(imagePosts)
                cursor = response.data.cursor

                // 十分な数の画像を取得できた場合は終了
                if (feeds.size >= limit) {
                    break
                }
            }
            Mapper.timelineByFeeds(
                feeds,
                paging,
                service()
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun searchTimeLine(
        query: String,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response = auth.accessor.feed().searchPosts(
                FeedSearchPostsRequest(accessJwt(), query).also {
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )

            Mapper.timelineByPosts(
                response.data.posts,
                paging,
                service()
            )
        }
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun postComment(
        req: CommentForm
    ) {
        proceedUnit {
            runBlocking {
                val imagesAsync = mutableListOf<Deferred<EmbedImagesImage>>()

                if (req.images.isNotEmpty()) {
                    req.images.map { img ->

                        // 画像を並列でアップロード実行
                        imagesAsync.add(async {
                            val response = auth.accessor.repo().uploadBlob(
                                RepoUploadBlobRequest(
                                    accessJwt = accessJwt(),
                                    bytes = img.data,
                                    name = img.name,
                                )
                            )

                            EmbedImagesImage().also {
                                it.image = response.data.blob
                                it.alt = ""
                            }
                        })
                    }
                }
                try {
                    // Facet を切り出して設定
                    val list = FacetUtil.extractFacets(req.text!!)
                    val facets = mutableListOf<RichtextFacet>()

                    if (list.records.isNotEmpty()) {
                        val handles = list.records
                            .filter { it.type == FacetType.Mention }
                            .map { it.contentText }

                        // Handle と Did の紐付けを作成
                        val handleDidMap = mutableMapOf<String, String>()
                        for (handle in handles) {
                            val response =
                                auth.accessor.identity().resolveHandle(
                                    IdentityResolveHandleRequest()
                                        .handle(handle.substring(1))
                                )
                            handleDidMap[handle] = response.data.did
                        }

                        // Facets のリストをここで生成
                        facets.addAll(list.richTextFacets(handleDidMap))
                    }

                    // 投稿リクエスト
                    val builder = FeedPostRequest(accessJwt()).also {
                        it.text = list.displayText()
                        it.facets = facets
                    }

                    // Images
                    var embedImages: EmbedImages? = null
                    if (imagesAsync.isNotEmpty()) {
                        val images = mutableListOf<EmbedImagesImage>()
                        for (imageAsync in imagesAsync) {
                            images.add(imageAsync.await())
                        }

                        embedImages = EmbedImages()
                        embedImages.images = images
                        builder.embed = embedImages
                    }

                    // Reply
                    if (req.replyId != null) {
                        val uri = req.replyId!!.value<String>()

                        // リプライルートを探索
                        // TODO: 深さがありえないぐらい深い場合はどうする？
                        val response = auth.accessor.feed().getPostThread(
                            FeedGetPostThreadRequest(accessJwt())
                                .also { it.uri = uri }
                        )

                        val union = response.data.thread
                        if (union is FeedDefsThreadViewPost) {
                            val reply = union.post
                            var parent = union.parent
                            var root: FeedDefsPostView? = null

                            // トップレベルの投稿を遡って変更
                            while (parent is FeedDefsThreadViewPost) {
                                root = parent.post
                                parent = parent.parent
                            }

                            // Root がない場合
                            if (root == null) {
                                root = reply
                            }

                            val rootRef = RepoStrongRef(root.uri!!, root.cid!!)
                            val replyRef = RepoStrongRef(reply.uri!!, reply.cid!!)

                            val ref = FeedPostReplyRef()
                            ref.parent = replyRef
                            ref.root = rootRef
                            builder.reply = ref
                        }
                    }

                    // Quote
                    if (req.quoteId != null) {
                        val uri = req.quoteId!!.value<String>()

                        val id = Identify(service(), ID(uri))
                        val comment = comment(id) as BlueskyComment

                        val record = EmbedRecord()
                        record.record = RepoStrongRef(uri, comment.cid!!)

                        // 既に画像が設定済みの場合
                        if (embedImages != null) {

                            // RecordWithMedia を生成して上書き設定
                            val rwm = EmbedRecordWithMedia()
                            rwm.media = embedImages
                            rwm.record = record
                            builder.embed = rwm

                        } else {
                            // 単純に Record を設定
                            builder.embed = record
                        }
                    }

                    auth.accessor.feed().post(builder)

                } catch (e: Exception) {
                    throw handleException(e)
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun comment(
        id: Identify
    ): Comment {
        return proceed {
            val posts = auth.accessor.feed().getPosts(
                FeedGetPostsRequest(accessJwt()).also {
                    it.uris = listOf(id.id!!.value())
                }
            )
            Mapper.simpleComment(
                posts.data.posts[0],
                service(),
            )
        }
    }

    private fun commentWithCheck(
        id: Identify
    ): BlueskyComment {
        return if (id is BlueskyComment) id
        else comment(id) as BlueskyComment
    }

    /**
     * {@inheritDoc}
     * https://bsky.app/profile/uakihir0.com/post/3jw2ydtuktc2j
     */
    override fun comment(
        url: String
    ): Comment {
        return proceed {
            try {
                val handle = Utils.userHandleFromUrl(url)
                val rkey = Utils.userRkeyFromUrl(url)

                val response = auth.accessor.identity().resolveHandle(
                    IdentityResolveHandleRequest().handle(handle)
                )

                val did = response.data.did
                val uri = "at://$did/app.bsky.feed.post/$rkey"

                val identify = Identify(service(), ID(uri))
                return@proceed comment(identify)

            } catch (e: Exception) {
                throw handleException(e)
            }
        }
    }

    /**
     * 25 投稿しか同時に取得できないので順々に取得
     * https://atproto.com/lexicons/app-bsky-feed#appbskyfeedgetposts
     */
    private fun postViews(
        uris: List<String>
    ): List<FeedDefsPostView> {

        var uriList = uris
        val results = mutableListOf<FeedDefsPostView>()

        if (uriList.isNotEmpty()) {
            do {
                val len = min(uriList.size, 25)
                val subUris = uriList.subList(0, len)
                uriList = uriList.subList(len, uriList.size)

                results.addAll(
                    auth.accessor.feed().getPosts(
                        FeedGetPostsRequest(accessJwt())
                            .also { it.uris = subUris }
                    ).data.posts
                )
            } while (uriList.isNotEmpty())
        }
        return results
    }

    /**
     * {@inheritDoc}
     */
    override fun likeComment(
        id: Identify
    ) {
        proceedUnit {
            val c = commentWithCheck(id)
            auth.accessor.feed().like(
                FeedLikeRequest(accessJwt())
                    .also { it.subject = c.ref() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unlikeComment(
        id: Identify
    ) {
        proceed {
            val c = commentWithCheck(id)
            auth.accessor.feed().deleteLike(
                FeedDeleteLikeRequest(accessJwt())
                    .also { it.uri = c.likeRecordUri }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun shareComment(
        id: Identify
    ) {
        proceed {
            val c = commentWithCheck(id)
            auth.accessor.feed().repost(
                FeedRepostRequest(accessJwt())
                    .also { it.subject = c.ref() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun unshareComment(
        id: Identify
    ) {
        proceed {
            val c = commentWithCheck(id)
            auth.accessor.feed().deleteRepost(
                FeedDeleteRepostRequest(accessJwt())
                    .also { it.uri = c.repostRecordUri }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun reactionComment(
        id: Identify,
        reaction: String,
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (BlueskyReactionType.Like.codes.contains(type)) {
                likeComment(id)
                return
            }
            if (BlueskyReactionType.Repost.codes.contains(type)) {
                shareComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override fun unreactionComment(
        id: Identify,
        reaction: String
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (BlueskyReactionType.Like.codes.contains(type)) {
                unlikeComment(id)
                return
            }
            if (BlueskyReactionType.Repost.codes.contains(type)) {
                unshareComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override fun deleteComment(
        id: Identify
    ) {
        proceed {
            auth.accessor.feed().deletePost(
                FeedDeletePostRequest(accessJwt())
                    .also { it.uri = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    fun commentContext(
        id: Identify
    ): Context {
        return proceed {
            val ancestors = mutableListOf<Comment>()
            val descendants = mutableListOf<Comment>()

            val response = auth.accessor.feed().getPostThread(
                FeedGetPostThreadRequest(accessJwt())
                    .also { it.uri = id.id!!.value() }
            )

            // 再帰的に確認を行い投稿リストを構築
            val union = response.data.thread
            if (union is FeedDefsThreadViewPost) {
                subGetCommentContext(union, ancestors, descendants, service())
            }

            Context().also {
                it.ancestors = ancestors
                it.descendants = descendants
                it.sort()
            }
        }
    }

    private fun subGetCommentContext(
        post: FeedDefsThreadViewPost,
        ancestors: MutableList<Comment>,
        descendants: MutableList<Comment>,
        service: Service
    ) {
        if (post.parent is FeedDefsThreadViewPost) {
            val parent = post.parent as FeedDefsThreadViewPost
            ancestors.add(Mapper.simpleComment(parent.post, service))
            subGetCommentContext(parent, ancestors, descendants, service)
        }

        if (post.replies != null && post.replies!!.isNotEmpty()) {
            for (reply in post.replies!!) {
                if (reply is FeedDefsThreadViewPost) {
                    descendants.add(Mapper.simpleComment(reply.post, service))
                    subGetCommentContext(reply, ancestors, descendants, service)
                }
            }
        }
    }

    // ============================================================== //
    // Channel (List) API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun channels(
        id: Identify,
        paging: Paging,
    ): Pageable<Channel> {
        return proceed {

            // ページング指定があった場合結果は空
            if (paging is BlueskyPaging) {
                if (paging.latestRecord != null || paging.cursor != null) {
                    val results = Pageable<Channel>()
                    results.paging = paging
                    return@proceed results
                }
            }

            val preferences = auth.accessor.actor().getPreferences(
                ActorGetPreferencesRequest(accessJwt())
            )

            val uris = mutableListOf<String>()
            for (union in preferences.data.preferences) {
                if (union is ActorDefsSavedFeedsPref) {
                    uris.addAll(union.saved)
                }
            }

            // 結果が空の場合
            if (uris.isEmpty()) {
                val results = Pageable<Channel>()
                results.paging = paging
                return@proceed results
            }

            val feeds = auth.accessor.feed().getFeedGenerators(
                FeedGetFeedGeneratorsRequest(accessJwt())
                    .also { it.feeds = uris }
            )

            Mapper.channels(
                feeds.data.feeds,
                paging,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun channelTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {

        return proceed {
            val response = auth.accessor.feed().getFeed(
                FeedGetFeedRequest(accessJwt()).also {
                    it.feed = id.id!!.value()
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )
            Mapper.timelineByFeeds(
                response.data.feed,
                paging,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Suppress("unused", "UNUSED_PARAMETER")
    fun trends(
        limit: Int
    ): List<Trend> {
        throw NotImplementedError()
    }

    /**
     * {@inheritDoc}
     */
    @Suppress("unused")
    fun notification(
        paging: Paging
    ): Pageable<Notification> {
        return proceed {

            // 取得する通知の種類を指定
            val types = listOf("like", "repost", "follow")

            if (paging.count != null) paging.count = 20
            paging.count = min(paging.count!!, 20)

            val model = notifications(paging, types)

            // 空の場合
            if (model.notifications!!.isEmpty()) {
                val results = Pageable<Notification>()
                results.paging = paging
                return@proceed results
            }

            // 投稿を取得
            val subjects = model.notifications!!
                .mapNotNull { it.reasonSubject }
                .distinct()

            val results = Mapper.notifications(
                model.notifications!!,
                postViews(subjects),
                null,
                service(),
            )

            // ページング情報を上書きする (ヒントの追加)
            val pg = BlueskyPaging.fromPaging(paging)
            val id = Identify(service(), ID(model.first!!))
            pg.cursorHint = model.cursor
            pg.latestRecordHint = id
            results.paging = pg
            results
        }
    }

    /**
     * 通知取得 + ページング
     */
    private fun notifications(
        paging: Paging,
        types: List<String>
    ): NotificationStructure {
        return runBlocking {

            // 既読処理を別スレッドで実行
            val seen = async {
                auth.accessor.notification().updateSeen(
                    NotificationUpdateSeenRequest(accessJwt())
                )
            }

            val notifications = mutableListOf<NotificationListNotificationsNotification>()

            val limit = limit(paging)
            var cursor = cursor(paging)
            var first: String? = null
            var stop = false

            for (i in 0..9) {
                val response = auth.accessor.notification().listNotifications(
                    NotificationListNotificationsRequest(accessJwt()).also {
                        it.cursor = cursor
                        it.limit = 100
                    }
                )

                // 初期 ID の記録
                var list = response.data.notifications
                if (first == null && list.isNotEmpty()) {
                    first = list[0].uri
                }

                // ページング処理 (最新の取得済みレコードを確認)
                if (paging is BlueskyPaging) {

                    if (paging.latestRecord != null) {
                        val uri = paging.latestRecord!!.id!!.value<String>()
                        list = list.takeUntil { it.uri == uri }

                        // 処理を停止
                        stop = true
                    }
                }

                // リアクションのみを取得
                list = list.filter { types.contains(it.reason) }

                if (list.isEmpty()) {
                    // 空の場合はカーソルだけを更新して終了
                    cursor = response.data.cursor

                } else {
                    // ページング処理 (limit までデータを取得)
                    if (notifications.size + list.size > limit) {
                        list = list.subList(0, limit - notifications.size)
                        stop = true
                    }

                    // 次ページをみるためカーソルを作成
                    val last = list[list.size - 1]
                    val date = last.indexedAt.toInstant()
                    cursor = "${date.toEpochMilliseconds()}::${last.cid}"

                    // 追加に要素を追加
                    notifications.addAll(list)
                }

                // 終了判定
                if (stop || notifications.size >= limit) {
                    break
                }
            }

            seen.join()

            NotificationStructure().also {
                it.notifications = notifications
                it.cursor = cursor
                it.first = first
            }
        }
    }

    // ============================================================== //
    // Other
    // ============================================================== //
    /**
     * Get Users who likes specified post
     * 特定のポストをいいねしたユーザーを取得
     */
    fun usersFavoriteBy(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {

        return proceed {
            val response = auth.accessor.feed().getLikes(
                FeedGetLikesRequest(accessJwt()).also {
                    it.uri = id.id!!.value()
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )
            Mapper.users(
                response.data.likes.map { it.actor },
                response.data.cursor,
                paging,
                service(),
            )
        }
    }

    /**
     * Get Users who reposts specified post
     * 特定のポストをリポストしたユーザーを取得
     */
    fun usersRetweetBy(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val response = auth.accessor.feed().getRepostedBy(
                FeedGetRepostedByRequest(accessJwt()).also {
                    it.uri = id.id!!.value()
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )
            Mapper.users(
                response.data.repostedBy,
                response.data.cursor,
                paging,
                service(),
            )
        }
    }


    // ============================================================== //
    // Support
    // ============================================================== //
    /**
     * Get User Uri from Identify
     * ID からユーザー URI を取得
     */
    private fun userUri(
        id: Identify
    ): String {
        var uri: String? = null

        // ユーザーオブジェクトから取得
        if (id is BlueskyUser) {
            uri = id.followRecordUri
        }

        // DID から取得
        if (uri == null) {
            val response = auth.accessor.actor().getProfile(
                ActorGetProfileRequest(accessJwt())
                    .also { it.actor = id.id!!.value() }
            )

            // ユーザー情報にフォローしているかどうかが確認できる
            val state = response.data.viewer
            if (state?.following != null) {
                uri = state.following
            }
        }

        return checkNotNull(uri)
    }

    // ============================================================== //
    // Paging
    // ============================================================== //
    private fun cursor(paging: Paging?): String? {
        if (paging is BlueskyPaging) {
            return paging.cursor
        }
        return null
    }

    private fun limit(paging: Paging?): Int {
        val limit = paging?.count ?: 50

        // Bluesky のページングは基本的に 1-100 を指定
        if (limit < 1) return 1
        if (limit > 100) return 100
        return limit
    }

    private fun rkey(uri: String?): String? {
        if (uri == null) return null

        return try {
            ATUriParser.getRKey(uri)
        } catch (e: Exception) {
            uri
        }
    }

    private fun countLimitPaging(
        paging: Paging?,
        limit: Int,
    ): Paging {
        if (paging != null) {
            if (paging.count != null) {
                paging.count = limit
                return paging
            }

            paging.count = min(paging.count!!, limit)
            return paging
        }
        return Paging(limit)
    }

    // ============================================================== //
    // Session
    // ============================================================== //
    private fun createSession() {
        val response = auth.accessor.server().createSession(
            ServerCreateSessionRequest().also {
                it.identifier = auth.identifier
                it.password = auth.password
            }
        )

        this.did = response.data.did
        this.accessJwt = response.data.accessJwt

        val jwt = Utils.jwt(this.accessJwt!!)
        this.expireAt = jwt.exp.toLong()
    }

    private fun accessJwt(): String {

        // 初回アクセスの場合
        if (accessJwt == null) {
            createSession()
            return accessJwt!!
        }

        // 有効期限が切れている場合
        val now = Clock.System.now().toEpochMilliseconds() / 1000
        if (now > (expireAt!! + 60)) {
            createSession()
            return accessJwt!!
        }

        return accessJwt!!
    }

    private fun did(): String {
        if (did == null) createSession()
        return did!!
    }

    private fun service(): Service {
        return account.service
    }

    // ============================================================== //
    // Utils
    // ============================================================== //
    private fun <T> proceed(runner: () -> T): T {
        try {
            return runner()
        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    private fun proceedUnit(runner: () -> Unit) {
        try {
            runner()
        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    class NotificationStructure {
        var notifications: List<NotificationListNotificationsNotification>? = null
        var cursor: String? = null
        var first: String? = null
    }

    private fun handleException(
        e: Exception
    ): SocialHubException {
        if ((e is BlueskyException) && (e.message != null)) {
            return SocialHubException(e.message, e)
        }
        throw SocialHubException(e)
    }
}
