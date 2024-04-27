package work.socialhub.planetlink.tumblr.action

import com.tumblr.jumblr.types.Blog
import com.tumblr.jumblr.types.Photo
import com.tumblr.jumblr.types.PhotoPost
import com.tumblr.jumblr.types.Post
import com.tumblr.jumblr.types.QuotePost
import com.tumblr.jumblr.types.TextPost
import com.tumblr.jumblr.types.Theme
import com.tumblr.jumblr.types.Trail
import com.tumblr.jumblr.types.VideoPost
import net.socialhub.core.define.MediaType
import net.socialhub.core.model.Comment
import net.socialhub.core.model.Media
import net.socialhub.core.model.Pageable
import net.socialhub.core.model.Paging
import net.socialhub.core.model.Relationship
import net.socialhub.core.model.Service
import net.socialhub.core.model.User
import net.socialhub.core.model.common.AttributedString
import net.socialhub.core.model.common.xml.XmlConvertRule
import net.socialhub.core.model.common.xml.XmlDocument
import net.socialhub.core.model.common.xml.XmlTag
import net.socialhub.core.utils.XmlParseUtil
import net.socialhub.service.tumblr.define.TumblrIconSize
import net.socialhub.service.tumblr.model.TumblrComment
import net.socialhub.service.tumblr.model.TumblrPaging
import net.socialhub.service.tumblr.model.TumblrUser
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
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
            if (blog.isPrimary()) {
                return user(blog, service)
            }
        }

        val model: User = User(service)
        model.setName(user.getName())
        return model
    }

    /**
     * ユーザーマッピング
     * (ブログをユーザーと設定)
     * (User is user's blog)
     */
    fun user(
        blog: com.tumblr.jumblr.types.Blog,  //
        service: Service?
    ): User {
        val model: TumblrUser = TumblrUser(service)

        model.setName(blog.getName())
        model.setBlogTitle(blog.getTitle())

        // FIXME: 説明文が HTML のユーザーはなぜ？
        if (blog.getDescription() != null) {
            try {
                // Tumblr の自己紹介文はまず HTML で解釈
                model.setDescription(AttributedString.xhtml(blog.getDescription()))
            } catch (ignore: java.lang.Exception) {
                // 解釈に失敗した場合は単純なプレーンテキストとして解釈
                model.setDescription(AttributedString.plain(blog.getDescription()))
            }
        }

        val host: String = getBlogIdentify(blog)
        model.setIconImageUrl(getAvatarUrl(host, TumblrIconSize.S512))
        model.setScreenName(host)
        model.setId(host)

        model.setFollowersCount(blog.getFollowersCount())
        model.setPostsCount(blog.getPostCount())
        model.setLikesCount(blog.getLikeCount())
        model.setBlogUrl(blog.getUrl())

        val relationship: Relationship = Relationship()
        relationship.setFollowing((blog.getFollowed() === java.lang.Boolean.TRUE))
        relationship.setBlocking((blog.getIsBlockedFromPrimary() === java.lang.Boolean.TRUE))
        model.setRelationship(relationship)

        return model
    }

    /**
     * ユーザーマッピング
     * (そのブログの投稿に紐づくユーザーと設定)
     */
    fun user(
        post: com.tumblr.jumblr.types.Post,  //
        trails: Map<String?, Trail?>,  //
        service: Service?
    ): User {
        val model: User = user(post.getBlog(), service)

        if (trails.containsKey(post.getBlog().getName())) {
            val trail: Trail? = trails[post.getBlog().getName()]
            val themes: List<Theme> = trail.getBlog().getTheme()

            if (themes != null && !themes.isEmpty()) {
                model.setCoverImageUrl(themes[0].getHeaderImage())
            }
        }

        return model
    }

    /**
     * ユーザーマッピング
     * (そのブログの投稿に紐づくユーザーと設定)
     */
    fun reblogUser(
        post: com.tumblr.jumblr.types.Post,  //
        trails: Map<String?, Trail?>,  //
        service: Service?
    ): User {
        val model: TumblrUser = TumblrUser(service)

        val host: String = getBlogIdentify(post.getRebloggedRootUrl())
        val name: String = post.getRebloggedRootName()

        model.setIconImageUrl(getAvatarUrl(host, TumblrIconSize.S512))
        model.setScreenName(host)
        model.setName(name)
        model.setId(host)

        if (trails.containsKey(name)) {
            val trail: Trail? = trails[name]
            val themes: List<Theme> = trail.getBlog().getTheme()

            if (themes != null && !themes.isEmpty()) {
                model.setCoverImageUrl(themes[0].getHeaderImage())
            }
        }

        return model
    }

    // ============================================================== //
    // Comment
    // ============================================================== //
    /**
     * コメントマッピング
     */
    fun comment(
        post: com.tumblr.jumblr.types.Post,  //
        trails: Map<String?, Trail?>,  //
        service: Service?
    ): Comment {
        val model: TumblrComment = TumblrComment(service)
        model.setCreateAt(java.util.Date(post.getTimestamp() * 1000))
        model.setReblogKey(post.getReblogKey())
        model.setNoteCount(post.getNoteCount())
        model.setWebUrl(post.getPostUrl())

        model.setId(post.getId())
        model.setUser(user(post, trails, service))

        // リブログ情報を設定
        if (post.getRebloggedRootId() != null) {
            model.setSharedComment(reblogComment(post, trails, service))
            model.setMedias(java.util.ArrayList<E>())
        } else {
            // コンテンツを格納

            setMedia(model, post)
        }

        return model
    }

    /**
     * コメントマッピング
     * (Handle as shared post)
     */
    fun reblogComment(
        post: com.tumblr.jumblr.types.Post,  //
        trails: Map<String?, Trail?>,  //
        service: Service?
    ): Comment {
        val model: TumblrComment = TumblrComment(service)
        model.setCreateAt(java.util.Date(post.getTimestamp() * 1000))
        model.setReblogKey(post.getReblogKey())
        model.setNoteCount(post.getNoteCount())

        model.setId(post.getRebloggedRootId())
        model.setUser(reblogUser(post, trails, service))
        setMedia(model, post)

        return model
    }

    // ============================================================== //
    // Medias
    // ============================================================== //
    fun setMedia(
        model: TumblrComment,  //
        post: com.tumblr.jumblr.types.Post
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

        if (post is PhotoPost) {
            val photo: PhotoPost = post as PhotoPost
            model.getMedias().addAll(photos(photo.getPhotos()))

            if (model.getText() == null) {
                val str = removeSharedBlogLink(photo.getCaption())
                textMedia(model, str)
            }
        }
        if (post is TextPost) {
            val text: TextPost = post as TextPost

            if (model.getText() == null) {
                val str = removeSharedBlogLink(text.getBody())
                textMedia(model, str)
            }
        }
        if (post is VideoPost) {
            val video: VideoPost = post as VideoPost
            model.getMedias().add(video(video))

            if (model.getText() == null) {
                val str = removeSharedBlogLink(video.getCaption())
                textMedia(model, str)
            }
        }

        if (post is QuotePost) {
            val quote: QuotePost = post as QuotePost

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
        val xml: XmlDocument = XmlParseUtil.xhtml(str)
        model.setText(xml.toAttributedString(XmlConvertRule()))

        // 画像一覧を取得
        val imgTags: List<XmlTag> = xml.findXmlTag("img")
        for (imgTag in imgTags) {
            // 画像を追加

            val media: Media = Media()
            media.setType(MediaType.Image)
            media.setSourceUrl(imgTag.getAttributes().get("src"))
            media.setPreviewUrl(imgTag.getAttributes().get("src"))
            model.getMedias().add(media)
        }

        // 動画一覧を選択
        val videoTags: List<XmlTag> = xml.findXmlTag("video")
        for (videoTag in videoTags) {
            // さらにそこからソースタグを抽出

            val sourceTags: List<XmlTag> = videoTag.findXmlTag("source")
            if (sourceTags.size == 1) {
                val sourceTag: XmlTag = sourceTags[0]

                // 動画を追加
                val media: Media = Media()
                media.setType(MediaType.Movie)
                media.setSourceUrl(sourceTag.getAttributes().get("src"))
                media.setPreviewUrl(videoTag.getAttributes().get("poster"))
                model.getMedias().add(media)
            }
        }
    }

    /**
     * 画像マッピング
     */
    private fun photos(
        photos: List<Photo>
    ): List<Media> {
        val results: MutableList<Media> = java.util.ArrayList<Media>()
        for (photo in photos) {
            val media: Media = Media()
            media.setType(MediaType.Image)
            media.setSourceUrl(photo.getOriginalSize().getUrl())
            media.setPreviewUrl(photo.getOriginalSize().getUrl())
            results.add(media)
        }

        return results
    }

    /**
     * 動画マッピング
     */
    private fun video(
        video: VideoPost
    ): Media {
        val media: Media = Media()
        media.setType(MediaType.Movie)

        // VideoUrl or PermalinkUrl を選択
        media.setSourceUrl(video.getVideoUrl())
        if (media.getSourceUrl() == null) {
            media.setSourceUrl(video.getPermalinkUrl())
        }

        media.setPreviewUrl(video.getThumbnailUrl())
        return media
    }

    // ============================================================== //
    // Timelines
    // ============================================================== //
    /**
     * タイムラインマッピング
     */
    fun timeLine(
        posts: List<com.tumblr.jumblr.types.Post?>,  //
        service: Service?,  //
        paging: Paging?
    ): Pageable<Comment> {
        val model: Pageable<Comment> = Pageable()
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
    fun getUserIdentify(blogs: List<Blog?>): String? {
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
    fun getBlogIdentify(blog: Blog): String {
        return getBlogIdentify(blog.getUrl())
    }

    /**
     * ホスト名を取得
     * Get blog host from url
     */
    fun getBlogIdentify(blogUrl: String): String? {
        var host = getUrlHost(blogUrl)

        // ドメイン設定していないブログは www.tumblr.com になる (?)
        if (host != null && host == "www.tumblr.com") {
            val elements = blogUrl.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            // 最後の三要素について確認するので、ループ回数を制限
            for (i in 0 until (elements.size - 2)) {
                // https://www.tumblr.com/blog/view/{uid}/xxx の形式で UID を取得

                if (elements[i] == "blog" && elements[i + 1] == "view") {
                    host = host!!.replace("www", elements[i + 2])
                }
            }
        }
        return host
    }

    /**
     * ホスト名を取得
     * Get host from url
     */
    fun getUrlHost(url: String?): String? {
        return try {
            java.net.URL(url).getHost()
        } catch (ignore: java.lang.Exception) {
            null
        }
    }

    /**
     * ユーザーの画面マップを取得
     * Get Trail Map
     */
    fun getTrailMap(
        posts: List<com.tumblr.jumblr.types.Post?>
    ): Map<String?, Trail?> {
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
    fun getAvatarUrl(host: String, size: TumblrIconSize): String {
        return "https://api.tumblr.com/v2/blog/" + host + "/avatar/" + size.getSize()
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