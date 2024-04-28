package work.socialhub.planetlink.tumblr.action

import com.tumblr.jumblr.types.Blog
import com.tumblr.jumblr.types.Post
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.datetime.Instant
import net.socialhub.core.model.User
import net.socialhub.service.tumblr.model.TumblrPaging
import work.socialhub.ktumblr.entity.blog.Blog
import work.socialhub.ktumblr.entity.post.Post
import work.socialhub.ktumblr.entity.post.legacy.LegacyPhotoPost
import work.socialhub.ktumblr.entity.post.legacy.LegacyQuotePost
import work.socialhub.ktumblr.entity.post.legacy.LegacyTextPost
import work.socialhub.ktumblr.entity.post.legacy.LegacyVideoPost
import work.socialhub.ktumblr.entity.post.options.Photo
import work.socialhub.ktumblr.entity.trail.Trail
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedKind.IMAGE
import work.socialhub.planetlink.model.common.AttributedKind.VIDEO
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.error.SocialHubException
import work.socialhub.planetlink.tumblr.define.TumblrIconSize
import work.socialhub.planetlink.tumblr.define.TumblrIconSize.S512
import work.socialhub.planetlink.tumblr.expand.AttributedStringEx.tumblr
import work.socialhub.planetlink.tumblr.model.TumblrComment
import work.socialhub.planetlink.tumblr.model.TumblrUser
import work.socialhub.ktumblr.entity.user.User as KUser

object TumblrMapper {


    // ============================================================== //
    // User
    // ============================================================== //
    /**
     * ユーザーマッピング
     * (Primary のブログをユーザーと設定)
     * (User is user's primary blog)
     */
    fun user(
        user: KUser,
        service: Service
    ): User {

        // プライマリブログについての処理
        for (blog in user.blogs!!) {
            if (blog.isPrimary == true) {
                return user(blog, service)
            }
        }

        throw SocialHubException(
            "Primary blog is not found."
        )
    }

    /**
     * ユーザーマッピング
     * (ブログをユーザーと設定)
     * (User is user's blog)
     */
    fun user(
        blog: Blog,
        service: Service
    ): User {
        return TumblrUser(service).also {

            val host = blogIdentify(blog.url!!)
            it.id = ID(host)

            it.name = blog.name!!
            it.blogTitle = blog.title
            it.webUrl = blog.url!!

            it.likesCount = blog.likeCount
            it.followersCount = blog.followerCount
            it.postsCount = blog.postCount

            // 説明文は HTML で記述
            blog.description?.let { d ->
                it.description = AttributedString.tumblr(d)
            }

            // FIXME:
            it.iconImageUrl = blog.avatar?.get(0)?.url

            it.relationship = Relationship().also { r ->
                r.following = (blog.isFollowed == true)
                r.blocking = (blog.isBlockedFromPrimary == true)
            }
        }
    }

    /**
     * ユーザーマッピング
     * (そのブログの投稿に紐づくユーザーと設定)
     */
    fun user(
        post: Post,
        trails: Map<String, Trail>,
        service: Service
    ): User {
        return user(post.blog!!, service).also {
            val name = checkNotNull(post.blog?.name)

            if (trails.containsKey(name)) {
                val trail = trails[name]
                val theme = trail?.blog?.theme

                if (theme != null) {
                    it.coverImageUrl = theme.headerImage
                }
            }
        }
    }

    /**
     * ユーザーマッピング
     * (そのブログの投稿に紐づくユーザーと設定)
     */
    fun reblogUser(
        post: Post,
        trails: Map<String, Trail>,
        service: Service
    ): User {
        return TumblrUser(service).also {
            val host = blogIdentify(post.parentPostUrl!!)
            val name = blogName(post.parentPostUrl!!)

            it.id = ID(host)
            it.name = name
            it.iconImageUrl = avatarUrl(host, S512)

            if (trails.containsKey(name)) {
                val trail = trails[name]
                val theme = trail?.blog?.theme

                if (theme != null) {
                    it.coverImageUrl = theme.headerImage
                }
            }
        }
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * コメントマッピング
     */
    fun comment(
        post: Post,
        trails: Map<String, Trail>,
        service: Service,
    ): Comment {
        return TumblrComment(service).also {

            it.id = ID(post.idString!!)
            it.webUrl = post.postUrl!!

            // it.noteCount = post.noteCount
            it.reblogKey = post.reblogKey
            it.createAt = Instant.fromEpochSeconds(post.timestamp!!.toLong())

            it.user = user(post, trails, service)

            // リブログ情報を設定
            if (post.parentPostUrl != null) {
                it.sharedComment = reblogComment(post, trails, service)
                it.medias = listOf()

            } else {
                // コンテンツを格納
                setMedia(it, post)
            }
        }
    }

    /**
     * コメントマッピング
     * (Handle as shared post)
     */
    fun reblogComment(
        post: Post,
        trails: Map<String, Trail>,
        service: Service
    ): Comment {
        return TumblrComment(service).also {
            it.id = ID(post.sourceUrl())

            it.noteCount = post.noteCount
            it.reblogKey = post.reblogKey
            it.createAt = Instant.fromEpochSeconds(post.timestamp!!.toLong())


            model.setId(post.getRebloggedRootId())
            model.setUser(reblogUser(post, trails, service))
            setMedia(model, post)

            return model
        }
    }

    // ============================================================== //
    // Medias
    // ============================================================== //
    fun setMedia(
        model: TumblrComment,
        post: Post
    ) {
        model.setMedias(java.util.ArrayList<E>())

        // Trail から優先的に取得
        if (post.getTrail() != null) {
            if (post.getTrail().size() > 0) {
                post.getTrail().stream()
                    .filter(Trail::isCurrentItem)
                    .findFirst().ifPresent { trail ->
                        textMedia(
                            model,
                            removeSharedBlogLink(trail.getContentRaw())
                        )
                    }
            }
        }

        if (post is LegacyPhotoPost) {
            model.getMedias().addAll(photos(photo.getPhotos()))

            if (model.getText() == null) {
                val str = removeSharedBlogLink(photo.getCaption())
                textMedia(model, str)
            }
        }
        if (post is LegacyTextPost) {
            if (model.getText() == null) {
                val str = removeSharedBlogLink(text.getBody())
                textMedia(model, str)
            }
        }
        if (post is LegacyVideoPost) {
            model.getMedias().add(video(video))

            if (model.getText() == null) {
                val str = removeSharedBlogLink(video.getCaption())
                textMedia(model, str)
            }
        }

        if (post is LegacyQuotePost) {
            if (model.getText() == null) {
                val str = removeSharedBlogLink(
                    quote.getText() + " / " + quote.getSource()
                )
                textMedia(model, str)
            }
        }
    }

    /**
     * テキスト情報を設定
     */
    private fun textMedia(
        model: TumblrComment,
        str: String
    ) {
        val medias = mutableListOf<Media>()
        val attr = AttributedString.tumblr(str)
            .also { model.text = it }

        val images = attr.elements.filter { it.kind == IMAGE }
        val videos = attr.elements.filter { it.kind == VIDEO }

        // 画像一覧を取得
        for (image in images) {
            medias.add(Media().also {
                it.type = MediaType.Image
                it.sourceUrl = image.expandedText
                it.previewUrl = image.displayText
            })
        }

        // 動画一覧を選択
        for (video in videos) {
            medias.add(Media().also {
                it.type = MediaType.Movie
                it.sourceUrl = video.expandedText
                it.previewUrl = video.displayText
            })
        }
    }

    /**
     * 画像マッピング
     */
    private fun photos(
        photos: List<Photo>
    ): List<Media> {
        return mutableListOf<Media>().also { medias ->
            for (photo in photos) {
                medias.add(Media().also {
                    it.type = MediaType.Image
                    it.sourceUrl = photo.originalSize?.url
                    it.previewUrl = photo.originalSize?.url
                })
            }
        }
    }

    /**
     * 動画マッピング
     */
    private fun video(
        video: LegacyVideoPost
    ): Media {
        return Media().also {
            it.type = MediaType.Movie

            // VideoUrl or PermalinkUrl を選択
            it.sourceUrl = video.getVideoUrl()
            if (video.sourceUrl == null) {
                it.sourceUrl = video.getPermalinkUrl()
            }

            it.previewUrl = video.getThumbnailUrl()
        }
    }

    // ============================================================== //
    // Timelines
    // ============================================================== //
    /**
     * タイムラインマッピング
     */
    fun timeLine(
        posts: Array<Post>,
        service: Service,
        paging: Paging?
    ): Pageable<Comment> {
        val model = Pageable<Comment>()
        val trails: Map<String?, Trail?> = getTrailMap(posts)

        model.setEntities(
            posts.stream() //
                .map<Any>(java.util.function.Function<com.tumblr.jumblr.types.Post, Any> { e: com.tumblr.jumblr.types.Post ->
                    comment(
                        e,
                        trails,
                        service
                    )
                }) //
                .sorted(java.util.Comparator.comparing<Any, Any>(Comment::getCreateAt).reversed()) //
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(TumblrPaging.fromPaging(paging))
        return model
    }

    // ============================================================== //
    // UserList
    // ============================================================== //
    /**
     * ユーザーマッピング
     */
    fun users(
        users: List<com.tumblr.jumblr.types.User?>,
        service: Service?,
        paging: Paging?
    ): Pageable<User> {
        val model: Pageable<User> = Pageable()
        model.setEntities(
            users.stream()
                .map<Any>(java.util.function.Function<com.tumblr.jumblr.types.User, Any> { e: com.tumblr.jumblr.types.User? ->
                    user(
                        e,
                        service
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(TumblrPaging.fromPaging(paging))
        return model
    }

    /**
     * ユーザーマッピング
     */
    fun usersByBlogs(
        blogs: List<com.tumblr.jumblr.types.Blog?>,  //
        service: Service?,  //
        paging: Paging?
    ): Pageable<User> {
        val model: Pageable<User> = Pageable()
        model.setEntities(
            blogs.stream() //
                .map<Any>(java.util.function.Function<com.tumblr.jumblr.types.Blog, Any> { e: com.tumblr.jumblr.types.Blog? ->
                    user(
                        e,
                        service
                    )
                }) //
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(TumblrPaging.fromPaging(paging))
        return model
    }

    // ============================================================== //
    // Supports
    // ============================================================== //
    /**
     * ホスト名を取得 (プライマリを取得)
     * Get primary blog host from url
     */
    fun userIdentify(blogs: Array<Blog>): String? {
        for (blog in blogs) {
            if (blog.isPrimary()) {
                return getBlogIdentify(blog)
            }
        }
        return null
    }

    /**
     * ホスト名を取得
     * Get blog host from url
     */
    fun blogIdentify(
        blog: Blog
    ): String {
        return blogIdentify(blog.url!!)
    }

    /**
     * ホスト名を取得
     * Get blog host from url
     */
    fun blogIdentify(
        blogUrl: String
    ): String {
        var host = blogUrl.urlHost()

        // ドメイン設定していないブログは www.tumblr.com になる (?)
        if (host == "www.tumblr.com") {
            val elements = blogUrl.split("/")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            // 最後の三要素について確認するので、ループ回数を制限
            for (i in 0..<(elements.size - 2)) {

                // https://www.tumblr.com/blog/view/{uid}/xxx の形式で UID を取得
                if (elements[i] == "blog" && elements[i + 1] == "view") {
                    host = host.replace("www", elements[i + 2])
                }
            }
        }
        return host
    }

    fun blogName(
        blogUrl: String
    ): String {
        return blogIdentify(blogUrl).split(".")[0]
    }

    /**
     * ホスト名を取得
     * Get host from url
     */
    private fun String.urlHost() =
        Url(this).host

    /**
     * ユーザーの画面マップを取得
     * Get Trail Map
     */
    fun getTrailMap(
        posts: Array<Post>
    ): Map<String, Trail> {
        posts.mapNotNull { it.trail }
        val trails: List<Trail> = posts.stream()
            .map<Any>(Post::getTrail)
            .filter(java.util.function.Predicate<Any> { obj: Any? -> java.util.Objects.nonNull(obj) })
            .flatMap<Any>(java.util.function.Function<Any, java.util.stream.Stream<*>> { obj: Collection<*> -> obj.stream() })
            .collect<List<Trail>, Any>(java.util.stream.Collectors.toList<Any>())

        val results: MutableMap<String?, Trail?> = java.util.HashMap<String, Trail>()
        for (trail in trails) {
            results[trail.getBlog().getName()] = trail
        }
        return results
    }

    /**
     * ユーザー情報のマージ処理
     */
    fun margeUser(to: User?, from: User?) {
        if ((to != null) && (from != null)) {
            to.setCoverImageUrl(from.getCoverImageUrl())
        }
    }

    /**
     * アバター画像を取得
     * Get avatar url
     */
    fun avatarUrl(
        host: String,
        size: TumblrIconSize
    ): String {
        return "https://api.tumblr.com/v2/blog/$host/avatar/${size.size}"
    }

    /**
     * Remove Shared Post Prefix
     * Share されたポストの定型句を削除
     */
    private fun removeSharedBlogLink(text: String): String {
        val regex = "^(<p><a(.+?)href=\"(.+?)\"(.*?)>(.+?)</a>:</p>)"
        val p: java.util.regex.Pattern = java.util.regex.Pattern.compile(regex)
        val m: java.util.regex.Matcher = p.matcher(text)

        if (m.find()) {
            val all: String = m.group(1)

            // ブログへのリンクであることを確認
            if (m.group(2).contains("class=\"tumblr_blog\"") ||
                m.group(4).contains("class=\"tumblr_blog\"")
            ) {
                return text.substring(all.length)
            }
        }
        return text
    }
}