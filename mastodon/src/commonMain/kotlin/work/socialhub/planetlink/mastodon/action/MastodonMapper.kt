package work.socialhub.planetlink.mastodon.action

import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import work.socialhub.kmastodon.api.response.Response
import work.socialhub.kmastodon.api.response.ResponseUnit
import work.socialhub.kmastodon.entity.Account
import work.socialhub.kmastodon.entity.AccountList
import work.socialhub.kmastodon.entity.Attachment
import work.socialhub.kmastodon.entity.Status
import work.socialhub.kmastodon.entity.share.Link
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.mastodon.define.MastodonMediaType
import work.socialhub.planetlink.mastodon.define.MastodonMediaType.Image
import work.socialhub.planetlink.mastodon.define.MastodonMediaType.Video
import work.socialhub.planetlink.mastodon.define.MastodonNotificationType
import work.socialhub.planetlink.mastodon.define.MastodonVisibility
import work.socialhub.planetlink.mastodon.expand.AttributedStringEx.mastodon
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
import work.socialhub.kmastodon.entity.Emoji as MEmoji
import work.socialhub.kmastodon.entity.Notification as MNotification
import work.socialhub.kmastodon.entity.Poll as MPoll
import work.socialhub.kmastodon.entity.Relationship as MRelationship

object MastodonMapper {

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

            // AvatarStatic が PixelFed 等では存在しない
            u.iconImageUrl = account.avatarStatic ?: account.avatar
            // HeaderStatic が PixelFed 等では存在しない
            u.coverImageUrl = account.headerStatic ?: account.header

            u.emojis = emojis(account.emojis)

            // ユーザー説明分の設定
            u.description = AttributedString.mastodon(account.note)
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
                    value = AttributedString.mastodon(f.value!!)
                        .also { it.addEmojiElement(u.emojis) }
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
                c.text = AttributedString.mastodon(status.content)
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
        list: AccountList,
        service: Service,
    ): Channel {
        return Channel(service).also {
            it.id = ID(list.id!!)
            it.name = list.title
            it.isPublic = false
        }
    }

    /**
     * 通知マッピング
     */
    fun notification(
        notification: MNotification,
        service: Service,
    ): Notification {
        return Notification(service).also { n ->
            n.id = ID(notification.id!!)
            n.createAt = parseDate(notification.createdAt!!)

            val type = MastodonNotificationType.of(notification.type!!)

            // 存在する場合のみ設定
            type?.let { t ->
                n.type = t.code
                t.action?.let { a ->
                    n.action = a.code
                }
            }

            // ステータス情報
            notification.status?.let { s ->
                n.comments = listOf(comment(s, service))
            }

            // ユーザー情報
            notification.account?.let { a ->
                n.users = listOf(user(a, service))
            }
        }
    }

    // ============================================================== //
    // List Object Mapper
    // ============================================================== //

    /**
     * タイムラインマッピング
     */
    fun timeLine(
        statuses: Array<Status>,
        service: Service,
        paging: Paging?,
        link: Link?
    ): Pageable<Comment> {
        return Pageable<Comment>().also { p ->
            p.entities = statuses
                .map { comment(it, service) }
                .sortedByDescending { it.createAt }

            val mpg = MastodonPaging.fromPaging(paging)
            p.paging = withLink(mpg, link)
        }
    }

    /**
     * ユーザーマッピング
     */
    fun users(
        accounts: Array<Account>,
        service: Service,
        paging: Paging?,
        link: Link?
    ): Pageable<User> {
        return Pageable<User>().also { p ->
            p.entities = accounts.map { user(it, service) }
            val mpg = MastodonPaging.fromPaging(paging)
            p.paging = withLink(mpg, link)
        }
    }

    /**
     * チャンネルマッピング
     */
    fun channels(
        lists: Array<AccountList>,
        service: Service,
    ): Pageable<Channel> {
        return Pageable<Channel>().also { p ->
            p.entities = lists.map { channel(it, service) }
        }
    }

    /**
     * 通知マッピング
     */
    fun notifications(
        notifications: Array<MNotification>,
        service: Service,
        paging: Paging?,
        link: Link?,
    ): Pageable<Notification> {
        return Pageable<Notification>().also { p ->
            p.entities = notifications.map { notification(it, service) }
            val mpg = MastodonPaging.fromPaging(paging)
            p.paging = withLink(mpg, link)
        }
    }

    /**
     * 絵文字マッピング
     */
    fun emoji(
        emoji: MEmoji
    ): Emoji {
        return Emoji().also {
            it.emoji = emoji.shortcode
            it.imageUrl = emoji.staticUrl
            it.category = emoji.category
            it.addShortCode(emoji.shortcode!!)
        }
    }

    /**
     * 絵文字マッピング
     */
    fun emojis(
        emojis: Array<MEmoji>?
    ): List<Emoji> {
        if (emojis == null) {
            return listOf()
        }
        return emojis.map { emoji(it) }
    }

    // ============================================================== //
    // Paging
    // ============================================================== //
    /**
     * add link paging options.
     */
    fun withLink(
        mbp: MastodonPaging,
        link: Link?,
    ): MastodonPaging {
        if (link != null) {
            mbp.minIdInLink = link.prevMinId
            mbp.maxIdInLink = link.nextMaxId
        }
        return mbp
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    private fun parseDate(
        str: String
    ): Instant {
        // TODO: 動作確認
        return str.toInstant()
    }

    fun rateLimit(
        response: Response<*>
    ): RateLimit.RateLimitValue? {

        // PixelFed は RateLimit に未対応
        response.limit?.let {
            return RateLimit.RateLimitValue(
                "Mastodon",
                it.limit,
                it.remaining,
                it.reset,
            )
        }
        return null
    }

    fun rateLimit(
        response: ResponseUnit
    ): RateLimit.RateLimitValue? {

        // PixelFed は RateLimit に未対応
        response.limit?.let {
            return RateLimit.RateLimitValue(
                "Mastodon",
                it.limit,
                it.remaining,
                it.reset,
            )
        }
        return null
    }
}
