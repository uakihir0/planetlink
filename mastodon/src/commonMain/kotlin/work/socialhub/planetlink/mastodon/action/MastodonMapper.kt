package work.socialhub.planetlink.mastodon.action

import io.ktor.http.*
import kotlinx.datetime.Instant
import work.socialhub.kmastodon.api.response.Response
import work.socialhub.kmastodon.entity.Account
import work.socialhub.kmastodon.entity.Attachment
import work.socialhub.kmastodon.entity.Status
import work.socialhub.kmastodon.entity.share.Link
import work.socialhub.kmpcommon.DateFormatter
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.mastodon.define.MastodonMediaType
import work.socialhub.planetlink.mastodon.define.MastodonMediaType.Image
import work.socialhub.planetlink.mastodon.define.MastodonMediaType.Video
import work.socialhub.planetlink.mastodon.define.MastodonVisibility
import work.socialhub.planetlink.mastodon.model.MastodonComment
import work.socialhub.planetlink.mastodon.model.MastodonPaging
import work.socialhub.planetlink.mastodon.model.MastodonPoll
import work.socialhub.planetlink.mastodon.model.MastodonUser
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedItem
import work.socialhub.planetlink.model.common.AttributedKind.ACCOUNT
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.support.PollOption
import work.socialhub.kmastodon.entity.Application as MApplication
import work.socialhub.kmastodon.entity.Poll as MPoll
import work.socialhub.kmastodon.entity.Relationship as MRelationship

/*
object MastodonMapper {

    fun rateLimit(
        response: Response<*>
    ): RateLimit.RateLimitValue {
        TODO()
    }

    fun rateLimit(
        response: ResponseUnit
    ): RateLimit.RateLimitValue {
        TODO()
    }

    fun user(
        account: Account,
        service: Service,
    ): User {
        TODO()
    }

    fun relationship(
        relationship: MRelationship
    ): Relationship {
        TODO()
    }

    fun users(
        accounts: Array<Account>,
        service: Service,
        paging: Paging,
        link: Link?
    ): Pageable<User> {
        TODO()
    }

    fun timeLine(
        statuses: Array<Status>,
        service: Service,
        paging: Paging,
        link: Link?
    ): Pageable<Comment> {
        TODO("Not yet implemented")
    }

    fun comment(
        status: Status,
        service: Service
    ): Comment {
        TODO()
    }

    fun emojis(
        emojis: Array<MEmoji>
    ): Collection<Emoji> {
        TODO("Not yet implemented")
    }

    fun channels(
        accountLists: Array<AccountList>,
        service: Service,
    ): Pageable<Channel> {
        TODO("Not yet implemented")
    }

    fun withLink(
        mpg: MastodonPaging,
        link: Link?
    ) {
    }

    fun notifications(
        notifications: Array<MNotification>,
        service: Service,
        paging: Paging,
        link: Link?,
    ): Pageable<Notification> {
        TODO("Not yet implemented")
    }

    fun notification(
        notification: MNotification,
        service: Service
    ): Notification {
        TODO()
    }
}

*/

object MastodonMapper {

    /** 時間のパーサーオブジェクト  */
    private val dateParsers = mutableMapOf<String, DateFormatter>()

    /** 時間のフォーマットの種類一覧  */
    private val DATE_FORMATS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )

    // ============================================================== //
    // Single Object Mapper
    // ============================================================== //
    /**
     * ユーザーマッピング
     */
    fun user(
        account: Account,
        service: Service
    ): User {
        return MastodonUser(service).also { u ->

            u.id = ID(account.id)
            u.name = account.displayName
            u.screenName = account.account

            // FIXME: AvatarStatic を参照したい
            // TODO: AvatarStatic が PixelFed 等では存在しない
            u.iconImageUrl = account.avatar
            // FIXME: HeaderStatic を参照したい
            // TODO: HeaderStatic が PixelFed 等では存在しない
            u.coverImageUrl = account.header

            u.emojis = emojis(account.emojis)

            // ユーザー説明分の設定
            u.description = AttributedString.plain(account.note) // TODO: XML
                .also { it.addEmojiElement(u.emojis) }

            u.followersCount = account.followersCount
            u.followingCount = account.followingCount
            u.statusesCount = account.statusesCount

            u.isProtected = account.isLocked

            // プロフィールページの設定
            u.webUrl = account.url

            u.fields = account.fields.map { f ->
                AttributedFiled(
                    name = f.name,
                    value = AttributedString.plain(f.value).also { // TODO: XML
                        it.addEmojiElement(u.emojis)
                    }
                )
            }
        }
    }

    /**
     * ユーザー関係
     */
    fun relationship(
        relationship: MRelationship
    ): Relationship {
        return Relationship().also {
            it.followed = relationship.isFollowedBy
            it.following = relationship.isFollowing
            it.blocking = relationship.isBlocking
            it.muting = relationship.isMuting
        }
    }

    /**
     * コメントマッピング
     */
    fun comment(
        status: Status,
        service: Service
    ): Comment {
        return MastodonComment(service).also { c ->

            c.id = ID(status.id)
            c.user = user(status.account, service)
            c.createAt = parseDate(status.createdAt)
            c.application = application(status.application)
            c.visibility = MastodonVisibility.of(status.visibility)
            c.possiblySensitive = status.isSensitive

            c.likeCount = status.favouritesCount
            c.shareCount = status.reblogsCount
            c.liked = status.isFavourited
            c.shared = status.isReblogged

            // リプライの数はメンションの数を参照
            c.replyCount = status.mentions.size

            // リツイートの場合は内部を展開
            if (status.reblog != null) {
                val reblog = checkNotNull(status.reblog)
                c.sharedComment = comment(reblog, service)
                c.medias = emptyList()

            } else {
                // 絵文字の追加
                c.emojis = emojis(status.emojis)

                // 注釈の設定
                c.spoilerText = AttributedString.plain(status.spoilerText)
                    .also { it.addEmojiElement(c.emojis) }

                // 本文の設定
                c.text = AttributedString.plain(status.content) // TODO: XML
                    .also { it.addEmojiElement(c.emojis) }

                // メンションの設定
                status.mentions.forEach { mention ->
                    c.text!!.elements.forEach { elem ->
                        if (elem.kind == ACCOUNT && elem is AttributedItem) {
                            if (elem.expandedText == mention.url) {
                                // アカウントの ID を URL のフラグメントに埋込
                                val url = "${mention.url}#${mention.id}"
                                elem.expandedText = url
                            }
                        }
                    }
                }

                // メディアの設定
                c.medias = medias(status.mediaAttachments)

                // 投票の設定
                c.poll = poll(status.poll, service)

                // リクエストホストを記録
                val url = Url(service.apiHost!!)
                c.requesterHost = url.host
            }
        }
    }

    /**
     * メディアマッピング
     */
    fun medias(
        attachments: Array<Attachment>
    ): List<Media> {
        return mutableListOf<Media>().also {
            for (attachment in attachments) {
                it.add(media(attachment))
            }
        }
    }

    /**
     * メディアマッピング
     */
    fun media(
        attachment: Attachment
    ): Media {
        return Media().also {
            it.sourceUrl = attachment.url
            it.previewUrl = attachment.previewUrl

            val type = MastodonMediaType.of(attachment.type!!)
            it.type = when (type) {
                Image -> MediaType.Image
                Video -> MediaType.Movie
                else -> MediaType.Other
            }
        }
    }

    /**
     * 投票マッピング
     */
    fun poll(
        poll: MPoll?,
        service: Service,
    ): Poll? {
        if (poll == null) {
            return null
        }

        return MastodonPoll(service).also { p ->
            p.id = ID(poll.id!!)
            p.isVoted = poll.isVoted
            p.isMultiple = poll.isMultiple
            p.isExpired = poll.isExpired

            p.votesCount = poll.votesCount
            p.votersCount = poll.votersCount

            // 通行期限
            poll.expiresAt?.let {
                p.expireAt = parseDate(it)
            }

            // 絵文字の追加
            p.emojis = emojis(poll.emojis)

            // 投票候補の追加
            val options = mutableListOf<PollOption>()
            p.options = options

            poll.options?.withIndex()
                ?.forEach { (index, option) ->
                    val op = PollOption(
                        // 投票のインデックスを記録
                        index = index,
                        title = option.title ?: "",
                        count = option.votesCount ?: 0
                    ).also { options.add(it) }

                    // 投票済みかどうかを確認
                    poll.ownVotes?.let { v ->
                        op.isVoted = v.any { it == op.index }
                    }
                }
        }
    }

    /**
     * アプリケーションマッピング
     */
    fun application(
        application: MApplication?
    ): Application? {
        if (application == null) {
            return null
        }
        return Application().also {
            it.name = application.name
            it.website = application.website
        }
    }

    /**
     * チャンネルマッピング
     */
    fun channel(
        list: mastodon4j.entity.List,
        service: Service?
    ): Channel {
        val channel = Channel(service!!)

        channel.setId(list.getId())
        channel.setName(list.getTitle())
        channel.setPublic(false)
        return channel
    }

    /**
     * 通知マッピング
     */
    fun notification(
        notification: mastodon4j.entity.Notification,
        service: Service?
    ): Notification {
        val model = Notification(service!!)
        model.setCreateAt(MastodonMapper.parseDate(notification.getCreatedAt()))
        model.setId(notification.getId())

        val type: MastodonNotificationType =
            MastodonNotificationType
                .of(notification.getType())

        // 存在する場合のみ設定
        if (type != null) {
            model.setType(type.getCode())
            if (type.getAction() != null) {
                model.setAction(type.getAction().getCode())
            }
        }

        // ステータス情報
        if (notification.getStatus() != null) {
            model.setComments(
                listOf(
                    comment(notification.getStatus(), service)
                )
            )
        }

        // ユーザー情報
        if (notification.getAccount() != null) {
            model.setUsers(
                listOf(
                    MastodonMapper.user(notification.getAccount(), service)
                )
            )
        }
        return model
    }

    // ============================================================== //
    // List Object Mapper
    // ============================================================== //
    /**
     * タイムラインマッピング
     */
    fun timeLine(
        statuses: Array<Status?>,
        service: Service?,
        paging: Paging?,
        link: Link?
    ): Pageable<Comment> {
        return timeLine(
            java.util.Arrays.asList(*statuses),
            service,
            paging,
            link
        )
    }

    /**
     * タイムラインマッピング
     */
    fun timeLine(
        statuses: List<Status?>,
        service: Service?,
        paging: Paging?,
        link: Link?
    ): Pageable<Comment> {
        val model: Pageable<Comment> = Pageable()
        model.setEntities(
            statuses.stream().map<Any>(java.util.function.Function<Status, Any> { e: Status? ->
                comment(
                    e!!, service!!
                )
            })
                .sorted(java.util.Comparator.comparing<Any, Any>(Comment::getCreateAt).reversed())
                .collect(java.util.stream.Collectors.toList())
        )

        val mpg = MastodonPaging.fromPaging(paging)
        model.setPaging(MastodonMapper.withLink(mpg, link))
        return model
    }

    /**
     * ユーザーマッピング
     */
    fun users(
        accounts: Array<Account?>?,
        service: Service?,
        paging: Paging?,
        link: Link?
    ): Pageable<User> {
        val model: Pageable<User> = Pageable()
        model.setEntities(
            java.util.stream.Stream.of<Array<Account>>(accounts)
                .map<Any>(java.util.function.Function<Array<Account>, Any> { a: Array<Account?>? ->
                    MastodonMapper.user(
                        a,
                        service!!
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(MastodonMapper.withLink(MastodonPaging.fromPaging(paging), link))
        return model
    }

    /**
     * チャンネルマッピング
     */
    fun channels(
        lists: Array<mastodon4j.entity.List?>?,
        service: Service?
    ): Pageable<Channel> {
        val model: Pageable<Channel> = Pageable()
        model.setEntities(
            java.util.stream.Stream.of<Array<mastodon4j.entity.List>>(lists)
                .map<Any>(java.util.function.Function<Array<mastodon4j.entity.List>, Any> { e: Array<mastodon4j.entity.List?>? ->
                    MastodonMapper.channel(
                        e,
                        service
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )
        return model
    }

    /**
     * 通知マッピング
     */
    fun notifications(
        notifications: Array<mastodon4j.entity.Notification?>?,
        service: Service?,
        paging: Paging?,
        link: Link?
    ): Pageable<Notification> {
        val model: Pageable<Notification> = Pageable()
        model.setEntities(
            java.util.stream.Stream.of<Array<mastodon4j.entity.Notification>>(notifications)
                .map<Any>(java.util.function.Function<Array<mastodon4j.entity.Notification>, Any> { a: Array<mastodon4j.entity.Notification?>? ->
                    MastodonMapper.notification(
                        a,
                        service!!
                    )
                })
                .collect(java.util.stream.Collectors.toList())
        )

        model.setPaging(MastodonMapper.withLink(MastodonPaging.fromPaging(paging), link))
        return model
    }

    /**
     * 絵文字マッピング
     */
    fun emoji(
        emoji: mastodon4j.entity.Emoji
    ): Emoji {
        val model = Emoji()
        model.addShortCode(emoji.getShortcode())
        model.setImageUrl(emoji.getStaticUrl())
        return model
    }

    /**
     * 絵文字マッピング
     */
    fun emojis(
        emojis: Array<mastodon4j.entity.Emoji?>?
    ): List<Emoji> {
        if (emojis == null) {
            return java.util.ArrayList<Emoji>()
        }
        return java.util.stream.Stream.of<Array<mastodon4j.entity.Emoji>>(emojis)
            .map(java.util.function.Function<Array<mastodon4j.entity.Emoji>, R> { obj: MastodonMapper?, emoji: mastodon4j.entity.Emoji? ->
                MastodonMapper.emoji(
                    emoji
                )
            })
            .collect<List<Emoji>, Any>(java.util.stream.Collectors.toList<Any>())
    }

    /**
     * XHtml 変換規則
     */
    fun xmlConvertRule(): XmlConvertRule {
        val rule: XmlConvertRule = XmlConvertRule()
        rule.setP("\n\n")
        return rule
    }

    // ============================================================== //
    // Paging
    // ============================================================== //
    /**
     * add link paging options.
     */
    fun withLink(mbp: MastodonPaging, link: Link?): MastodonPaging {
        if (link != null) {
            mbp.setMinIdInLink(link.getPrevMinId())
            mbp.setMaxIdInLink(link.getNextMaxId())
        }
        return mbp
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    fun parseDate(str: String): Instant {
        for (dateFormat in MastodonMapper.DATE_FORMATS) {
            var dateParser: SimpleDateFormat? = MastodonMapper.dateParsers.get(dateFormat)

            if (dateParser == null) {
                dateParser = SimpleDateFormat(dateFormat)
                dateParser.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
                MastodonMapper.dateParsers.put(dateFormat, dateParser)
            }
            try {
                return dateParser.parse(str)
            } catch (ignore: java.text.ParseException) {
            }
        }
        throw java.lang.IllegalStateException("Unparseable date: $str")
    }

    fun rateLimit(response: Response<*>): RateLimitValue? {
        // PixelFed は RateLimit に未対応

        if (response.getRateLimit() != null) {
            val rateLimit: mastodon4j.entity.share.RateLimit = response.getRateLimit()
            return RateLimitValue(
                ServiceType.Mastodon,
                rateLimit.getLimit(),
                rateLimit.getRemaining(),
                rateLimit.getReset()
            )
        }
        return null
    }
}
