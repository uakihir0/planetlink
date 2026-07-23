package work.socialhub.planetlink.bluesky.action

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorGetPreferencesRequest
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorGetProfileRequest
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorSearchActorsRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedCreateBookmarkRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedDeleteBookmarkRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedDeleteLikeRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedDeletePostRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedDeleteRepostRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetFeedGeneratorsRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetFeedRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetLikesRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetPostThreadRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetPostsRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetRepostedByRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetTimelineRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedLikeRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedRepostRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedSearchPostsRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphBlockRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphDeleteBlockRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphDeleteFollowRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphFollowRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphGetFollowersRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphGetFollowsRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphMuteActorRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphUnmuteActorRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphCreateListRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphAddUserToListRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphGetListRequest
import work.socialhub.kbsky.api.entity.app.bsky.graph.GraphRemoveUserFromListRequest
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorUpdateProfileRequest
import work.socialhub.kbsky.api.entity.com.atproto.moderation.ModerationCreateReportRequest
import work.socialhub.kbsky.model.com.atproto.moderation.ModerationReasonType
import work.socialhub.kbsky.model.com.atproto.repo.RepoRef
import work.socialhub.kbsky.model.share.Blob
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationUpdateSeenRequest
import work.socialhub.kbsky.api.entity.com.atproto.identity.IdentityResolveHandleRequest
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoListRecordsRequest
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoUploadBlobRequest
import work.socialhub.kbsky.api.entity.com.atproto.server.ServerCreateSessionRequest
import work.socialhub.kbsky.auth.AuthProvider
import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.kbsky.stream.BlueskyStreamFactory
import work.socialhub.kbsky.stream.api.entity.app.bsky.JetStreamSubscribeRequest
import work.socialhub.kbsky.stream.entity.app.bsky.callback.JetStreamEventCallback
import work.socialhub.kbsky.stream.entity.app.bsky.model.Event
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsSavedFeedsPref
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternalExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedGallery
import work.socialhub.kbsky.model.app.bsky.embed.EmbedGalleryImage
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImagesImage
import work.socialhub.kbsky.model.app.bsky.embed.EmbedRecord
import work.socialhub.kbsky.model.app.bsky.embed.EmbedRecordWithMedia
import work.socialhub.kbsky.model.app.bsky.embed.EmbedUnion
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsThreadViewPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedLike
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.kbsky.model.app.bsky.richtext.RichtextFacet
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import work.socialhub.kbsky.util.ATUriParser
import work.socialhub.kbsky.util.facet.FacetType
import work.socialhub.kbsky.util.facet.FacetUtil
import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.action.Capabilities
import work.socialhub.planetlink.action.RequestAction
import work.socialhub.planetlink.bluesky.define.BlueskyReactionType
import work.socialhub.planetlink.define.action.MessageActionType
import work.socialhub.planetlink.define.action.SocialActionType
import work.socialhub.planetlink.define.action.StreamActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.bluesky.model.BlueskyComment
import work.socialhub.planetlink.bluesky.model.BlueskyPaging
import work.socialhub.planetlink.bluesky.model.BlueskyStream
import work.socialhub.planetlink.bluesky.model.BlueskyUser
import work.socialhub.planetlink.bluesky.support.Utils
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.action.callback.comment.UpdateCommentCallback
import work.socialhub.planetlink.action.callback.lifecycle.ConnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.DisconnectCallback
import work.socialhub.planetlink.action.callback.lifecycle.ErrorCallback
import net.socialhub.planetlink.model.event.CommentEvent
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Notification
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Stream
import work.socialhub.planetlink.model.Thread
import work.socialhub.planetlink.model.Trend
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.define.ServiceType
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.utils.ExceptionHandler
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.LinkForm
import work.socialhub.planetlink.model.request.MediaForm
import work.socialhub.planetlink.model.request.ProfileForm
import work.socialhub.planetlink.bluesky.model.BlueskyChannel
import work.socialhub.planetlink.utils.CollectionUtil.takeUntil
import kotlin.js.JsExport
import kotlin.math.min
import work.socialhub.planetlink.bluesky.action.BlueskyMapper as Mapper

/** Bluesky プラットフォームのアクション実装 */
@JsExport
class BlueskyAction(
    account: Account,
    val auth: BlueskyAuth,
) : AccountActionImpl(account) {

    companion object {
        const val MAX_WANTED_DIDS_PER_CONNECTION = 300

        val CAPABILITIES = Capabilities(
            setOf(
                SocialActionType.GetUserMe,
                SocialActionType.GetUser,
                SocialActionType.FollowUser,
                SocialActionType.UnfollowUser,
                SocialActionType.MuteUser,
                SocialActionType.UnmuteUser,
                SocialActionType.BlockUser,
                SocialActionType.UnblockUser,
                SocialActionType.GetRelationship,
                SocialActionType.GetComment,
                SocialActionType.GetContext,
                SocialActionType.PostComment,
                SocialActionType.DeleteComment,
                SocialActionType.LikeComment,
                SocialActionType.UnlikeComment,
                SocialActionType.ShareComment,
                SocialActionType.UnShareComment,
                SocialActionType.ReactionComment,
                SocialActionType.UnreactionComment,
                SocialActionType.GetChannels,
                SocialActionType.GetNotification,
                SocialActionType.BookmarkComment,
                SocialActionType.UnbookmarkComment,
                SocialActionType.ReportUser,
                SocialActionType.ReportComment,
                SocialActionType.UpdateProfile,
                SocialActionType.CreateList,
                SocialActionType.AddUserToList,
                SocialActionType.RemoveUserFromList,
                SocialActionType.MarkNotificationsRead,

                TimeLineActionType.HomeTimeLine,
                TimeLineActionType.MentionTimeLine,
                TimeLineActionType.UserCommentTimeLine,
                TimeLineActionType.UserLikeTimeLine,
                TimeLineActionType.UserMediaTimeLine,
                TimeLineActionType.SearchTimeLine,
                TimeLineActionType.ChannelTimeLine,

                UsersActionType.GetFollowingUsers,
                UsersActionType.GetFollowerUsers,
                UsersActionType.SearchUsers,

                StreamActionType.HomeTimeLineStream,
                StreamActionType.NotificationStream,
            )
        )
    }

    override fun capabilities(): Capabilities = CAPABILITIES

    private var accessJwt: String? = null
    private var expireAt: Long? = null
    private var did: String? = null

    // ============================================================== //
    // Account
    // ============================================================== //

    /**
     * {@inheritDoc}
     */
    override suspend fun userMe(): User {
        return fetchUserMe()
    }

    // Free-standing impl + userMeWithCache override so the base-class
    // userMeWithCache() -> userMe() virtual suspend bridge (unwired on JS) is
    // never reached. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun fetchUserMe(): User {
        return proceed {
            val response = auth.accessor.actor().getProfile(
                ActorGetProfileRequest(authProvider())
                    .also { it.actor = did() }
            )

            Mapper.user(response.data, service())
                .also { this.me = it }
        }
    }

    override suspend fun userMeWithCache(): User {
        return me ?: fetchUserMe()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun user(
        id: Identify
    ): User {
        return proceed {
            val response = auth.accessor.actor().getProfile(
                ActorGetProfileRequest(authProvider())
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
    override suspend fun user(
        url: String
    ): User {
        return proceed {
            val id = Utils.userIdentifyFromUrl(url)
            val response = auth.accessor.actor().getProfile(
                ActorGetProfileRequest(authProvider())
                    .also { it.actor = id }
            )
            Mapper.user(response.data, service())
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun followUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().follow(
                GraphFollowRequest(authProvider())
                    .also { it.subject = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unfollowUser(
        id: Identify
    ) {
        val uri = userUri(id)
        proceedUnit {
            auth.accessor.graph().deleteFollow(
                GraphDeleteFollowRequest(authProvider())
                    .also { it.uri = uri }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun muteUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().muteActor(
                GraphMuteActorRequest(authProvider())
                    .also { it.actor = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unmuteUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().unmuteActor(
                GraphUnmuteActorRequest(authProvider())
                    .also { it.actor = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun blockUser(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.graph().block(
                GraphBlockRequest(authProvider())
                    .also { it.subject = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unblockUser(
        id: Identify
    ) {
        val uri = userUri(id)
        proceedUnit {
            auth.accessor.graph().deleteBlock(
                GraphDeleteBlockRequest(authProvider())
                    .also { it.uri = uri }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun relationship(
        id: Identify
    ): Relationship {
        return proceed {
            if (id is BlueskyUser) {
                Mapper.relationship(id)
            } else {
                // Fetch profile directly instead of calling user(id) to avoid
                // broken virtual suspend bridge issue in KMP JS target
                val response = auth.accessor.actor().getProfile(
                    ActorGetProfileRequest(authProvider())
                        .also { it.actor = id.id!!.value() }
                )
                val user = Mapper.user(response.data, service())
                Mapper.relationship(user as BlueskyUser)
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
    override suspend fun followingUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val follows =
                auth.accessor.graph().getFollows(
                    GraphGetFollowsRequest(authProvider()).also {
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
    override suspend fun followerUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val follows =
                auth.accessor.graph().getFollowers(
                    GraphGetFollowersRequest(authProvider()).also {
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
    override suspend fun searchUsers(
        query: String,
        paging: Paging
    ): Pageable<User> {
        return proceed {
            val response =
                auth.accessor.actor().searchActors(
                    ActorSearchActorsRequest(authProvider()).also {
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
    override suspend fun homeTimeLine(
        paging: Paging
    ): Pageable<Comment> {
        return proceed {
            val response = auth.accessor.feed().getTimeline(
                FeedGetTimelineRequest(authProvider()).also {
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )

            Mapper.timelineByFeeds(
                response.data.feed,
                response.data.cursor,
                paging,
                service()
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun mentionTimeLine(
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
    override suspend fun userCommentTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response =
                auth.accessor.feed().getAuthorFeed(
                    FeedGetAuthorFeedRequest(authProvider()).also {
                        it.actor = id.id!!.value()
                        it.cursor = cursor(paging)
                        it.limit = limit(paging)
                    }
                )
            Mapper.timelineByFeeds(
                response.data.feed,
                response.data.cursor,
                paging,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun userLikeTimeLine(
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
    override suspend fun userMediaTimeLine(
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
                        FeedGetAuthorFeedRequest(authProvider()).also {
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
                            if (embed is EmbedGallery) {
                                return@filter embed.items!!.isNotEmpty()
                            }
                        }
                        false
                    }

                feeds.addAll(imagePosts)
                cursor = response.data.cursor

                // 十分な数の画像を取得できた場合は終了
                if (feeds.size >= limit) break
            }
            Mapper.timelineByFeeds(
                feeds,
                cursor,
                paging,
                service()
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun searchTimeLine(
        query: String,
        paging: Paging,
    ): Pageable<Comment> {
        return proceed {
            val response = auth.accessor.feed().searchPosts(
                FeedSearchPostsRequest(authProvider(), query).also {
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )

            Mapper.timelineByPosts(
                response.data.posts,
                response.data.cursor,
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
    override suspend fun postComment(
        req: CommentForm
    ) {
        proceedUnit {
            coroutineScope {
                val blobsAsync = mutableListOf<Deferred<Blob>>()
                val link = req.link

                if (req.images.isNotEmpty()) {
                    req.images.map { img ->

                        // 画像を並列でアップロード実行
                        blobsAsync.add(async {
                            val response = auth.accessor.repo().uploadBlob(
                                RepoUploadBlobRequest(
                                    auth = authProvider(),
                                    bytes = img.data,
                                    name = img.name,
                                )
                            )
                            response.data.blob
                        })
                    }
                }
                val linkThumbnailAsync = if (req.images.isEmpty()) {
                    link?.thumbnail?.let { thumbnail ->
                        async {
                            val response = auth.accessor.repo().uploadBlob(
                                RepoUploadBlobRequest(
                                    auth = authProvider(),
                                    bytes = thumbnail.data,
                                    name = thumbnail.name,
                                )
                            )
                            response.data.blob
                        }
                    }
                } else {
                    null
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
                    val builder = FeedPostRequest(authProvider()).also {
                        it.text = list.displayText()
                        it.facets = facets
                    }

                    // Images (up to 4) or Gallery (5-10)
                    var embedMedia: EmbedUnion? = null
                    if (blobsAsync.isNotEmpty()) {
                        val blobs = blobsAsync.map { it.await() }

                        if (blobs.size <= 4) {
                            val embedImages = EmbedImages()
                            embedImages.images = blobs.mapIndexed { index, blob ->
                                EmbedImagesImage().also {
                                    it.image = blob
                                    it.alt = req.images[index].description ?: ""
                                    it.aspectRatio = aspectRatio(req.images[index])
                                }
                            }
                            embedMedia = embedImages
                        } else {
                            val embedGallery = EmbedGallery()
                            embedGallery.items = blobs.mapIndexed { index, blob ->
                                EmbedGalleryImage(
                                    image = blob,
                                    alt = req.images[index].description ?: "",
                                    aspectRatio = aspectRatio(req.images[index]),
                                )
                            }
                            embedMedia = embedGallery
                        }
                        builder.embed = embedMedia
                    } else if (link != null) {
                        embedMedia = linkEmbed(
                            link,
                            linkThumbnailAsync?.await(),
                        )
                        builder.embed = embedMedia
                    }

                    // Reply
                    if (req.replyId != null) {
                        val uri = req.replyId!!.value<String>()

                        // リプライルートを探索
                        // TODO: 深さがありえないぐらい深い場合はどうする？
                        val response = auth.accessor.feed().getPostThread(
                            FeedGetPostThreadRequest(authProvider())
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

                            val rootRef = RepoStrongRef(root?.uri!!, root.cid!!)
                            val replyRef = RepoStrongRef(reply?.uri!!, reply.cid!!)

                            val ref = FeedPostReplyRef()
                            ref.parent = replyRef
                            ref.root = rootRef
                            builder.reply = ref
                        }
                    }

                    // Quote
                    if (req.quoteId != null) {
                        val uri = req.quoteId!!.value<String>()

                        // Fetch post directly to avoid broken virtual suspend bridge
                        val quotePosts = auth.accessor.feed().getPosts(
                            FeedGetPostsRequest(authProvider()).also {
                                it.uris = listOf(uri)
                            }
                        )
                        val quoteComment = Mapper.simpleComment(
                            quotePosts.data.posts[0], service()
                        ) as BlueskyComment

                        val record = EmbedRecord()
                        record.record = RepoStrongRef(uri, quoteComment.cid!!)

                        // 既に画像が設定済みの場合
                        if (embedMedia != null) {

                            // RecordWithMedia を生成して上書き設定
                            val rwm = EmbedRecordWithMedia()
                            rwm.media = embedMedia
                            rwm.record = record
                            builder.embed = rwm

                        } else {
                            // 単純に Record を設定
                            builder.embed = record
                        }
                    }

                    auth.accessor.feed().post(builder)

                } catch (e: Exception) {
                    throw ExceptionHandler.classify(e, ServiceType.Bluesky,
                        statusCode = (e as? ATProtocolException)?.status,
                        responseBody = (e as? ATProtocolException)?.body)
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun comment(
        id: Identify
    ): Comment {
        return proceed {
            val posts = auth.accessor.feed().getPosts(
                FeedGetPostsRequest(authProvider()).also {
                    it.uris = listOf(id.id!!.value())
                }
            )
            Mapper.simpleComment(
                posts.data.posts[0],
                service(),
            )
        }
    }

    private suspend fun commentWithCheck(
        id: Identify
    ): BlueskyComment {
        if (id is BlueskyComment) return id
        return proceed {
            val posts = auth.accessor.feed().getPosts(
                FeedGetPostsRequest(authProvider()).also {
                    it.uris = listOf(id.id!!.value())
                }
            )
            Mapper.simpleComment(
                posts.data.posts[0],
                service(),
            ) as BlueskyComment
        }
    }

    // ============================================================== //
    // Report / Profile / List / Notification (SocialHub additions)
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override suspend fun reportUser(
        id: Identify,
        comment: String?,
    ) {
        proceedUnit {
            val did = resolveDid(id)
            auth.accessor.moderation().createReport(
                ModerationCreateReportRequest(
                    reasonType = ModerationReasonType.OTHER,
                    reason = comment,
                    subject = RepoRef(did),
                    auth = authProvider(),
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reportComment(
        id: Identify,
        comment: String?,
    ) {
        val c = commentWithCheck(id)
        proceedUnit {
            auth.accessor.moderation().createReport(
                ModerationCreateReportRequest(
                    reasonType = ModerationReasonType.OTHER,
                    reason = comment,
                    subject = c.ref(),
                    auth = authProvider(),
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun updateProfile(
        form: ProfileForm
    ) {
        proceedUnit {
            var avatarBlob: Blob? = null
            var bannerBlob: Blob? = null

            if (form.avatar != null) {
                val response = auth.accessor.repo().uploadBlob(
                    RepoUploadBlobRequest(
                        auth = authProvider(),
                        bytes = form.avatar!!,
                        name = form.avatarName ?: "avatar",
                    )
                )
                avatarBlob = response.data.blob
            }

            if (form.banner != null) {
                val response = auth.accessor.repo().uploadBlob(
                    RepoUploadBlobRequest(
                        auth = authProvider(),
                        bytes = form.banner!!,
                        name = form.bannerName ?: "banner",
                    )
                )
                bannerBlob = response.data.blob
            }

            auth.accessor.actor().updateProfile(
                ActorUpdateProfileRequest(
                    auth = authProvider(),
                    displayName = form.displayName,
                    description = form.description,
                    avatar = avatarBlob,
                    banner = bannerBlob,
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun createList(
        name: String,
        description: String?,
    ): Channel {
        return proceed {
            val response = auth.accessor.graph().createList(
                GraphCreateListRequest(
                    auth = authProvider(),
                    name = name,
                    description = description,
                )
            )

            BlueskyChannel(service()).also {
                it.id = ID(response.data.uri)
                it.cid = response.data.cid
                it.name = name
                it.description = description
                it.isPublic = true
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun addUserToList(
        channel: Identify,
        user: Identify,
    ) {
        proceedUnit {
            val userDid = resolveDid(user)
            auth.accessor.graph().addUserToList(
                GraphAddUserToListRequest(authProvider()).also {
                    it.userDid = userDid
                    it.listUri = channel.id!!.value<String>()
                }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun removeUserFromList(
        channel: Identify,
        user: Identify,
    ) {
        proceedUnit {
            val userDid = resolveDid(user)
            val listUri = channel.id!!.value<String>()

            // list item の record uri を探索 (delete には listitem レコード URI が必要)
            var cursor: String? = null
            var targetUri: String? = null
            do {
                val response = auth.accessor.graph().getList(
                    GraphGetListRequest(authProvider()).also {
                        it.list = listUri
                        it.limit = 100
                        it.cursor = cursor
                    }
                )
                targetUri = response.data.items
                    .firstOrNull { it.subject?.did == userDid }?.uri
                cursor = response.data.cursor
            } while (targetUri == null && cursor != null)

            checkNotNull(targetUri) { "User is not a member of the list." }
            auth.accessor.graph().removeUserFromList(
                GraphRemoveUserFromListRequest(authProvider()).also {
                    it.uri = targetUri
                }
            )
        }
    }

    /**
     * {@inheritDoc}
     * Bluesky には per-id 既読がないため upToId は無視して全件既読にする。
     */
    override suspend fun markNotificationsRead(
        upToId: Identify?,
    ) {
        proceedUnit {
            auth.accessor.notification().updateSeen(
                NotificationUpdateSeenRequest(authProvider())
            )
        }
    }

    // BlueskyUser ids are already DIDs; a raw handle is resolved via
    // identity().resolveHandle. Private (different-class + private calls only)
    // so the JS suspend-bridge yield* bug is not triggered.
    private suspend fun resolveDid(id: Identify): String {
        val value = id.id!!.value<String>()
        if (value.startsWith("did:")) return value
        val response = auth.accessor.identity().resolveHandle(
            IdentityResolveHandleRequest().handle(value)
        )
        return response.data.did
    }

    /**
     * {@inheritDoc}
     * https://bsky.app/profile/uakihir0.com/post/3jw2ydtuktc2j
     */
    override suspend fun comment(
        url: String
    ): Comment {
        return proceed {
            val handle = Utils.userHandleFromUrl(url)
            val rkey = Utils.userRkeyFromUrl(url)

            val response = auth.accessor.identity().resolveHandle(
                IdentityResolveHandleRequest().handle(handle)
            )

            val did = response.data.did
            val uri = "at://$did/app.bsky.feed.post/$rkey"

            val posts = auth.accessor.feed().getPosts(
                FeedGetPostsRequest(authProvider()).also {
                    it.uris = listOf(uri)
                }
            )
            Mapper.simpleComment(
                posts.data.posts[0], service()
            )
        }
    }

    /**
     * 画像の幅・高さが指定されていれば aspectRatio に変換
     * (両方が正の値の場合のみ。指定が無ければ null で送らない)
     */
    private fun aspectRatio(media: MediaForm): EmbedDefsAspectRatio? {
        val width = media.width
        val height = media.height
        if (width != null && height != null && width > 0 && height > 0) {
            return EmbedDefsAspectRatio(width, height)
        }
        return null
    }

    /**
     * 25 投稿しか同時に取得できないので順々に取得
     * https://atproto.com/lexicons/app-bsky-feed#appbskyfeedgetposts
     */
    private suspend fun postViews(
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
                        FeedGetPostsRequest(authProvider())
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
    override suspend fun likeComment(
        id: Identify
    ) {
        doLikeComment(id)
    }

    // Free-standing impls so same-class callers (reactionComment) don't route
    // through the unwired JS virtual suspend bridge. See AGENTS.md "Kotlin/JS yield* Bug".
    private suspend fun doLikeComment(id: Identify) {
        val c = commentWithCheck(id)
        proceedUnit {
            auth.accessor.feed().like(
                FeedLikeRequest(authProvider())
                    .also { it.subject = c.ref() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unlikeComment(
        id: Identify
    ) {
        doUnlikeComment(id)
    }

    private suspend fun doUnlikeComment(id: Identify) {
        val c = commentWithCheck(id)
        proceed {
            auth.accessor.feed().deleteLike(
                FeedDeleteLikeRequest(authProvider())
                    .also { it.uri = c.likeRecordUri }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun shareComment(
        id: Identify
    ) {
        doShareComment(id)
    }

    private suspend fun doShareComment(id: Identify) {
        val c = commentWithCheck(id)
        proceed {
            auth.accessor.feed().repost(
                FeedRepostRequest(authProvider())
                    .also { it.subject = c.ref() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unshareComment(
        id: Identify
    ) {
        doUnshareComment(id)
    }

    private suspend fun doUnshareComment(id: Identify) {
        val c = commentWithCheck(id)
        proceed {
            auth.accessor.feed().deleteRepost(
                FeedDeleteRepostRequest(authProvider())
                    .also { it.uri = c.repostRecordUri }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun reactionComment(
        id: Identify,
        reaction: String,
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (BlueskyReactionType.Like.codes.contains(type)) {
                doLikeComment(id)
                return
            }
            if (BlueskyReactionType.Repost.codes.contains(type)) {
                doShareComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unreactionComment(
        id: Identify,
        reaction: String
    ) {
        if (reaction.isNotEmpty()) {
            val type = reaction.lowercase()

            if (BlueskyReactionType.Like.codes.contains(type)) {
                doUnlikeComment(id)
                return
            }
            if (BlueskyReactionType.Repost.codes.contains(type)) {
                doUnshareComment(id)
                return
            }
        }
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun deleteComment(
        id: Identify
    ) {
        proceed {
            auth.accessor.feed().deletePost(
                FeedDeletePostRequest(authProvider())
                    .also { it.uri = id.id!!.value() }
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun bookmarkComment(
        id: Identify
    ) {
        val c = commentWithCheck(id)
        proceedUnit {
            auth.accessor.feed().createBookmark(
                FeedCreateBookmarkRequest(
                    auth = authProvider(),
                    uri = c.id!!.value(),
                    cid = c.cid!!,
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun unbookmarkComment(
        id: Identify
    ) {
        proceedUnit {
            auth.accessor.feed().deleteBookmark(
                FeedDeleteBookmarkRequest(
                    auth = authProvider(),
                    uri = id.id!!.value(),
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    suspend fun commentContext(
        id: Identify
    ): Context {
        return proceed {
            val ancestors = mutableListOf<Comment>()
            val descendants = mutableListOf<Comment>()

            val response = auth.accessor.feed().getPostThread(
                FeedGetPostThreadRequest(authProvider())
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
            ancestors.add(Mapper.simpleComment(parent.post!!, service))
            subGetCommentContext(parent, ancestors, descendants, service)
        }

        if (post.replies != null && post.replies!!.isNotEmpty()) {
            for (reply in post.replies!!) {
                if (reply is FeedDefsThreadViewPost) {
                    descendants.add(Mapper.simpleComment(reply.post!!, service))
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
    override suspend fun channels(
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
                ActorGetPreferencesRequest(authProvider())
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
                FeedGetFeedGeneratorsRequest(authProvider())
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
    override suspend fun channelTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {

        return proceed {
            val response = auth.accessor.feed().getFeed(
                FeedGetFeedRequest(authProvider()).also {
                    it.feed = id.id!!.value()
                    it.cursor = cursor(paging)
                    it.limit = limit(paging)
                }
            )
            Mapper.timelineByFeeds(
                response.data.feed,
                response.data.cursor,
                paging,
                service(),
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun commentContexts(
        id: Identify
    ): Context {
        return commentContext(id)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun channelUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun messageThread(
        paging: Paging,
    ): Pageable<Thread> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun messageTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotSupportedException()
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun postMessage(
        req: CommentForm
    ) {
        throw NotSupportedException()
    }

    // ============================================================== //
    // Request
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun request(): RequestAction {
        return BlueskyRequest(account)
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    suspend fun homeTimeLineStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val profiles = getAllFollowingProfiles()
            val profileCache = profiles.associateBy { it.did }
            val followingDids = profiles.map { it.did } + did()

            val clients = followingDids
                .chunked(MAX_WANTED_DIDS_PER_CONNECTION)
                .map { chunk ->
                    val client = BlueskyStreamFactory
                        .instance()
                        .jetStream()
                        .subscribe(
                            JetStreamSubscribeRequest().also {
                                it.wantedCollections = listOf(BlueskyTypes.FeedPost)
                                it.wantedDids = chunk
                            }
                        )

                    client.eventCallback(object : JetStreamEventCallback {
                        override fun onEvent(event: Event) {
                            if (callback is UpdateCommentCallback) {
                                val commit = event.commit ?: return
                                if (commit.operation != "create") return

                                val comment = Mapper.commentFromEvent(event, service(), profileCache)
                                    ?: return
                                callback.onUpdate(CommentEvent(comment))
                            }
                        }
                    })

                    client.openedCallback(object : work.socialhub.kbsky.stream.entity.callback.OpenedCallback {
                        override fun onOpened() {
                            if (callback is ConnectCallback) {
                                callback.onConnect()
                            }
                        }
                    })

                    client.closedCallback(object : work.socialhub.kbsky.stream.entity.callback.ClosedCallback {
                        override fun onClosed() {
                            if (callback is DisconnectCallback) {
                                callback.onDisconnect()
                            }
                        }
                    })

                    client.errorCallback(object : work.socialhub.kbsky.stream.entity.callback.ErrorCallback {
                        override fun onError(e: Exception) {
                            if (callback is ErrorCallback) {
                                val classified = if (e is SocialHubException) e
                                    else ExceptionHandler.classify(e, ServiceType.Bluesky,
                                        statusCode = (e as? ATProtocolException)?.status
                                            ?: (e.cause as? ATProtocolException)?.status,
                                        responseBody = (e as? ATProtocolException)?.body
                                            ?: (e.cause as? ATProtocolException)?.body)
                                callback.onError(classified)
                            }
                        }
                    })

                    client
                }

            BlueskyStream(clients)
        }
    }

    override suspend fun setHomeTimeLineStream(
        callback: EventCallback
    ): Stream {
        return homeTimeLineStream(callback)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun setNotificationStream(
        callback: EventCallback
    ): Stream {
        return proceed {
            val myDid = did()
            val followingDids = getAllFollowingDids()

            val clients = followingDids
                .chunked(MAX_WANTED_DIDS_PER_CONNECTION)
                .map { chunk ->
                    val client = BlueskyStreamFactory
                        .instance()
                        .jetStream()
                        .subscribe(
                            JetStreamSubscribeRequest().also {
                                it.wantedCollections = listOf(
                                    BlueskyTypes.FeedLike,
                                    BlueskyTypes.FeedRepost,
                                    BlueskyTypes.GraphFollow,
                                )
                                it.wantedDids = chunk
                            }
                        )

                    client.eventCallback(object : JetStreamEventCallback {
                        override fun onEvent(event: Event) {
                            val commit = event.commit ?: return
                            if (commit.operation != "create") return
                            val record = commit.record ?: return

                            val subjectUri = when (record) {
                                is work.socialhub.kbsky.model.app.bsky.feed.FeedLike ->
                                    record.subject?.uri
                                is work.socialhub.kbsky.model.app.bsky.feed.FeedRepost ->
                                    record.subject?.uri
                                else -> null
                            }

                            // like/repost: subject が自分の投稿である場合のみ通知
                            if (subjectUri != null && !subjectUri.startsWith("at://$myDid/")) return

                            // follow: 自分がフォローされた場合のみ (JetStream では subject が record に含まれない)
                            // GraphFollow の wantedDids は followingDids なので、
                            // フォロー先が自分をフォローした場合に通知される
                        }
                    })

                    client.openedCallback(object : work.socialhub.kbsky.stream.entity.callback.OpenedCallback {
                        override fun onOpened() {
                            if (callback is ConnectCallback) {
                                callback.onConnect()
                            }
                        }
                    })

                    client.closedCallback(object : work.socialhub.kbsky.stream.entity.callback.ClosedCallback {
                        override fun onClosed() {
                            if (callback is DisconnectCallback) {
                                callback.onDisconnect()
                            }
                        }
                    })

                    client.errorCallback(object : work.socialhub.kbsky.stream.entity.callback.ErrorCallback {
                        override fun onError(e: Exception) {
                            if (callback is ErrorCallback) {
                                val classified = if (e is SocialHubException) e
                                    else ExceptionHandler.classify(e, ServiceType.Bluesky,
                                        statusCode = (e as? ATProtocolException)?.status
                                            ?: (e.cause as? ATProtocolException)?.status,
                                        responseBody = (e as? ATProtocolException)?.body
                                            ?: (e.cause as? ATProtocolException)?.body)
                                callback.onError(classified)
                            }
                        }
                    })

                    client
                }

            BlueskyStream(clients)
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
    override suspend fun notification(
        paging: Paging
    ): Pageable<Notification> {
        return proceed {

            // 取得する通知の種類を指定
            val types = listOf("like", "repost", "follow")

            if (paging.count == null) paging.count = 20
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
    private suspend fun notifications(
        paging: Paging,
        types: List<String>
    ): NotificationStructure {
        return coroutineScope {

            // 既読処理を別スレッドで実行
            val seen = async {
                auth.accessor.notification().updateSeen(
                    NotificationUpdateSeenRequest(authProvider())
                )
            }

            val notifications = mutableListOf<NotificationListNotificationsNotification>()

            val limit = limit(paging)
            var cursor = cursor(paging)
            var first: String? = null
            var stop = false

            for (i in 0..9) {
                val response = auth.accessor.notification().listNotifications(
                    NotificationListNotificationsRequest(authProvider()).also {
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
                        val uri = paging.latestRecord!!.id<String>()
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

                    // API レスポンスのカーソルを使用
                    cursor = response.data.cursor

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
    suspend fun usersFavoriteBy(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {

        return proceed {
            val response = auth.accessor.feed().getLikes(
                FeedGetLikesRequest(authProvider()).also {
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
    suspend fun usersRetweetBy(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        return proceed {
            val response = auth.accessor.feed().getRepostedBy(
                FeedGetRepostedByRequest(authProvider()).also {
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
     * フォロー中の全ユーザーの DID を取得
     */
    private suspend fun getAllFollowingDids(): List<String> {
        return getAllFollowingProfiles().map { it.did }
    }

    private suspend fun getAllFollowingProfiles(): List<ActorDefsProfileView> {
        val profiles = mutableListOf<ActorDefsProfileView>()
        var cursor: String? = null

        do {
            val response = auth.accessor.graph().getFollows(
                GraphGetFollowsRequest(authProvider()).also {
                    it.actor = did()
                    it.cursor = cursor
                    it.limit = 100
                }
            )
            if (profiles.isEmpty()) {
                profiles.add(response.data.subject)
            }
            profiles.addAll(response.data.follows)
            cursor = response.data.cursor
        } while (cursor != null)

        return profiles
    }

    /**
     * Get User Uri from Identify
     * ID からユーザー URI を取得
     */
    private suspend fun userUri(
        id: Identify
    ): String {
        return proceed {
            var uri: String? = null

            if (id is BlueskyUser) {
                uri = id.followRecordUri
            }

            if (uri == null) {
                val response = auth.accessor.actor().getProfile(
                    ActorGetProfileRequest(authProvider())
                        .also { it.actor = id.id!!.value() }
                )

                val state = response.data.viewer
                if (state?.following != null) {
                    uri = state.following
                }
            }

            checkNotNull(uri)
        }
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
            if (paging.count == null) {
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
    private suspend fun createSession() {
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

    private suspend fun authProvider(): AuthProvider {
        // 初回アクセスの場合
        if (accessJwt == null) {
            createSession()
            return BearerTokenAuthProvider(accessJwt!!)
        }

        // 有効期限が切れている場合
        val now = Clock.System.now().toEpochMilliseconds() / 1000
        if (now > (expireAt!! + 60)) {
            createSession()
            return BearerTokenAuthProvider(accessJwt!!)
        }

        return BearerTokenAuthProvider(accessJwt!!)
    }

    private suspend fun did(): String {
        if (did == null) createSession()
        return did!!
    }

    private fun service(): Service {
        return account.service
    }

    // ============================================================== //
    // Utils
    // ============================================================== //
    private suspend fun <T> proceed(runner: suspend () -> T): T {
        return ExceptionHandler.proceed(
            serviceType = ServiceType.Bluesky,
            statusExtractor = { e ->
                (e as? ATProtocolException)?.status
                    ?: (e.cause as? ATProtocolException)?.status
            },
            bodyExtractor = { e ->
                (e as? ATProtocolException)?.body
                    ?: (e.cause as? ATProtocolException)?.body
            },
            runner = runner,
        )
    }

    private suspend fun proceedUnit(runner: suspend () -> Unit) {
        ExceptionHandler.proceedUnit(
            serviceType = ServiceType.Bluesky,
            statusExtractor = { e ->
                (e as? ATProtocolException)?.status
                    ?: (e.cause as? ATProtocolException)?.status
            },
            bodyExtractor = { e ->
                (e as? ATProtocolException)?.body
                    ?: (e.cause as? ATProtocolException)?.body
            },
            runner = runner,
        )
    }

    class NotificationStructure {
        var notifications: List<NotificationListNotificationsNotification>? = null
        var cursor: String? = null
        var first: String? = null
    }
}

internal fun linkEmbed(
    link: LinkForm,
    thumbnail: Blob?,
): EmbedExternal {
    return EmbedExternal(
        external = EmbedExternalExternal(
            uri = link.uri,
            title = link.title,
            description = link.description,
            thumb = thumbnail,
        )
    )
}
