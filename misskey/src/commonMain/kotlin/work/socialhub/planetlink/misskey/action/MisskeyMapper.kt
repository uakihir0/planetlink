package work.socialhub.planetlink.misskey.action

import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import work.socialhub.kmisskey.entity.File
import work.socialhub.kmisskey.entity.Note
import work.socialhub.kmisskey.entity.NoteList
import work.socialhub.kmisskey.entity.Relation
import work.socialhub.kmpcommon.DateFormatter
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.misskey.define.MisskeyNotificationType
import work.socialhub.planetlink.misskey.define.MisskeyVisibility
import work.socialhub.planetlink.misskey.model.MisskeyComment
import work.socialhub.planetlink.misskey.model.MisskeyNotification
import work.socialhub.planetlink.misskey.model.MisskeyPaging
import work.socialhub.planetlink.misskey.model.MisskeyPoll
import work.socialhub.planetlink.misskey.model.MisskeyUser
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.support.Color
import work.socialhub.planetlink.model.support.PollOption
import work.socialhub.kmisskey.entity.Color as MColor
import work.socialhub.kmisskey.entity.Emoji as MEmoji
import work.socialhub.kmisskey.entity.Notification as MNotification
import work.socialhub.kmisskey.entity.Poll as MPoll
import work.socialhub.kmisskey.entity.Trend as MTrend
import work.socialhub.kmisskey.entity.user.User as MUser

object MisskeyMapper {

    private const val DATE_FORMAT = "yyyy-MM-ddTHH:mm:ss.SSSZ"

    // ============================================================== //
    /** 時間のパーサーオブジェクト  */
    var dateParser: DateFormatter? = null
        get() {
            if (field == null) {
                field = DateFormatter(DATE_FORMAT, TimeZone.UTC)
            }
            return field
        }

    // ============================================================== //
    // Single Object Mapper
    // ============================================================== //
    /**
     * ユーザーマッピング
     */
    fun user(
        account: MUser,
        host: String,
        service: Service
    ): User {
        return MisskeyUser(service).also { u ->
            val emojis = account.emojis?.list?.toList() ?: emptyList()
            val detailed = account.asUserDetailedNotMe

            u.id = ID(account.id)
            u.name = account.name!!
            u.host = account.host ?: host
            u.screenName = account.username
            u.iconImageUrl = account.avatarUrl

            u.emojis = selectEmojis(emojis, account.name, host)
            u.avatarColor = color(account.avatarColorObject)
            u.isSimple = (detailed == null)

            if (detailed != null) {

                u.location = detailed.location
                u.coverImageUrl = detailed.bannerUrl

                // ピンしているコメントの設定
                u.pinnedComments = detailed.pinnedNotes
                    .map { comment(it, host, service) }

                // ユーザー説明分の設定
                u.description = AttributedString.plain(detailed.description)
                    .also { it.addEmojiElement(selectEmojis(emojis, detailed.description, host)) }

                u.followersCount = detailed.followersCount
                u.followingCount = detailed.followingCount
                u.statusesCount = detailed.notesCount

                u.isProtected = detailed.isLocked
                u.isBot = detailed.isBot
                u.isCat = detailed.isCat


                // バナーの色設定
                // it.bannerColor = color(detailed.bannerColor)

                // フィールドの設定
                u.fields = detailed.fields.map { f ->
                    AttributedFiled(f.name, f.value).also { af ->
                        af.value?.addEmojiElement(selectEmojis(emojis, f.value, host))
                    }
                }
            }
        }
    }

    /**
     * コメントマッピング
     */
    fun comment(
        note: Note,
        host: String,
        service: Service
    ): Comment {
        return MisskeyComment(service).also { c ->
            val emojis = note.emojis?.list?.toList() ?: emptyList()
            val files = note.files?.toList() ?: emptyList()

            c.id = ID(note.id)
            c.user = user(note.user, host, service)
            c.createAt = note.createdAt.toInstant()
            c.visibility = MisskeyVisibility.Public

            c.shareCount = note.renoteCount
            c.replyCount = note.repliesCount
            c.directMessage = false

            // リツイートの場合は内部を展開
            note.renote?.let { note ->
                c.sharedComment = comment(note, host, service)
            }

            // 注釈の設定
            note.cw?.let { cw ->
                c.spoilerText = AttributedString.plain(cw)
                    .also { it.addEmojiElement(selectEmojis(emojis, cw, host)) }
            }

            // 本文の設定
            c.text = AttributedString.plain(note.text)
                .also { it.addEmojiElement(selectEmojis(emojis, note.text, host)) }

            // メディアの設定
            c.medias = medias(files)

            // Misskey ではファイル単位でセンシティブかを判断
            c.possiblySensitive = files.any { it.isSensitive }

            // 投票の設定
            c.poll = poll(note, note.poll, service)

            // リアクションの設定
            c.reactions = reactions(note.reactions, note.myReaction, host)

            // リクエストホストを記録
            val url = Url(service.apiHost!!)
            c.requesterHost = url.host
        }
    }

    /**
     * 通知メンションマッピング
     */
    fun mention(
        notification: MNotification,
        host: String,
        service: Service,
    ): Comment {
        val comment = comment(notification.note!!, host, service)
        (comment as MisskeyComment).pagingId = notification.id
        return comment
    }

    /**
     * ユーザー関係
     */
    fun relationship(
        relation: Relation
    ): Relationship {
        return Relationship().also {
            it.followed = relation.isFollowed
            it.following = relation.isFollowing
            it.blocking = relation.isBlocking
            it.muting = relation.isMuted
        }
    }

    /**
     * 絵文字マッピング
     */
    fun emoji(
        emoji: MEmoji
    ): Emoji {
        return Emoji().also {
            it.emoji = emoji.name
            it.imageUrl = emoji.url
            it.category = emoji.category
            it.shortCodes = emoji.aliases?.toList() ?: emptyList()
        }
    }

    /**
     * メディアマッピング
     */
    fun media(
        file: File
    ): Media? {

        // 画像の場合
        if (file.type.startsWith("image/")) {
            return Media().also {
                it.type = MediaType.Image
                it.sourceUrl = file.url
                // サムネイル画像が設定されている場合
                it.previewUrl = file.thumbnailUrl ?: file.url
            }
        }

        // 動画の場合
        if (file.type.startsWith("video/")) {
            return Media().also {
                it.type = MediaType.Movie
                it.sourceUrl = file.url
                // サムネイル画像が設定されている場合
                it.previewUrl = file.thumbnailUrl
            }
        }

        return null
    }

    /**
     * チャンネルマッピング
     */
    fun channel(
        list: NoteList,
        service: Service
    ): Channel {
        return Channel(service).also {
            it.id = ID(list.id!!)
            it.name = list.name
            it.createAt = list.createdAt?.toInstant()
            it.isPublic = false
        }
    }

    /**
     * 通知マッピング
     */
    fun notification(
        notification: MNotification,
        emojis: List<Emoji>,
        host: String,
        service: Service,
    ): Notification {
        return MisskeyNotification(service).also { n ->
            val type = MisskeyNotificationType.of(notification.type)

            n.id = ID(notification.id)
            n.reaction = notification.reaction

            // アンテナの通知などは時刻が含まれないので確認
            n.createAt = notification.createdAt.toInstant()

            // ローカルアイコンの取得
            n.iconUrl = emojis.firstOrNull {
                it.shortCodes.contains(notification.reaction)
            }?.imageUrl

            // リモートアイコンの取得
            notification.reaction?.let { reaction ->
                if (type == MisskeyNotificationType.REACTION &&
                    reaction.startsWith(":") &&
                    n.iconUrl == null
                ) {
                    val code = reaction.replace(":", "")
                    n.iconUrl = getEmojiURL(host, code)
                }
            }

            // 存在する場合のみ設定
            type?.let { nt ->
                n.type = nt.code
                n.action = nt.action?.code
            }

            // ステータス情報
            notification.note?.let {
                n.comments = listOf(comment(it, host, service))
            }

            // ユーザー情報
            notification.user?.let {
                n.users = listOf(user(it, host, service))
            }
        }
    }


    /**
     * 投票オブジェクトマッピング
     */
    fun poll(
        note: Note,
        poll: MPoll?,
        service: Service
    ): Poll? {
        if (poll == null) {
            return null
        }

        return MisskeyPoll(service).also { p ->
            p.noteId = note.id

            if (poll.expiresAt != null) {
                p.expireAt = poll.expiresAt!!.toInstant()
                p.isExpired = p.expireAt!! < Clock.System.now()
            } else {
                // 無期限の投票の場合
                p.isExpired = false
                p.expireAt = null
            }

            p.isMultiple = poll.multiple
            p.isVoted = poll.choices?.any { it.voted } ?: false
            p.votesCount = poll.choices?.sumOf { it.votes ?: 0 } ?: 0
            p.votersCount = null

            p.options = poll.choices?.mapIndexed { i, c ->
                PollOption(
                    index = i,
                    title = c.text ?: "",
                    count = c.votes ?: 0
                ).also { it.isVoted = c.voted }
            } ?: emptyList()
        }
    }

    /**
     * トレンドマッピング
     */
    fun trend(
        trend: MTrend
    ): Trend {
        return Trend().also {
            it.name = "#${trend.tag}"
            it.query = "#${trend.tag}"
            it.volume = trend.usersCount
        }
    }

    /**
     * カラーマッピング
     */
    fun color(
        color: MColor?
    ): Color? {
        if (color == null) return null
        return Color().also {
            it.r = color.r
            it.g = color.g
            it.b = color.b
            it.a = 255
        }
    }

    // ============================================================== //
    // List Object Mappers
    // ============================================================== //
    /**
     * ユーザーマッピング
     */
    fun users(
        accounts: List<MUser>,
        host: String,
        service: Service,
        paging: Paging?
    ): Pageable<User> {
        return Pageable<User>().also { pg ->
            pg.entities = accounts.map { user(it, host, service) }
            pg.paging = MisskeyPaging.fromPaging(paging)
        }
    }

    /**
     * タイムラインマッピング
     */
    fun timeLine(
        notes: List<Note>,
        host: String,
        service: Service,
        paging: Paging?
    ): Pageable<Comment> {
        return Pageable<Comment>().also { pg ->
            pg.entities = notes
                .map { comment(it, host, service) }
                .sortedBy { it.createAt }
                .reversed()
            pg.paging = MisskeyPaging.fromPaging(paging)
        }
    }

    /**
     * 通知メンションマッピング
     */
    fun mentions(
        notifications: List<MNotification>,
        host: String,
        service: Service,
        paging: Paging?
    ): Pageable<Comment> {
        return Pageable<Comment>().also { pg ->
            pg.entities = notifications.map { mention(it, host, service) }
                .sortedBy { it.createAt }
                .reversed()
            pg.paging = MisskeyPaging.fromPaging(paging)
        }
    }

    /**
     * リアクションマッピング
     */
    fun reactions(
        reactions: Map<String, Int>,
        myReaction: String?,
        host: String
    ): List<Reaction> {
        val models = mutableListOf<Reaction>()
        reactions.forEach { (k, v) ->

            // カスタム絵文字の場合
            if (k.startsWith(":")) {
                val name = k.split(":")
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1]

                // V13 からリモート絵文字の取得方法が変更
                Reaction().also {
                    it.reacting = k == myReaction
                    it.iconUrl = getEmojiURL(host, name)
                    it.name = k
                    it.count = v
                    models.add(it)
                }

            } else {
                // 一般的な絵文字の場合
                Reaction().also {
                    it.reacting = k == myReaction
                    it.emoji = k
                    it.name = k
                    it.count = v
                    models.add(it)
                }
            }
        }

        return models
    }

    /**
     * 絵文字マッピング
     */
    fun emojis(
        emojis: List<MEmoji>?
    ): List<Emoji> {
        if (emojis == null) {
            return listOf()
        }
        return emojis.map { emoji -> emoji(emoji) }
    }

    /**
     * メディアマッピング
     */
    fun medias(
        files: List<File>
    ): List<Media> {
        return files.mapNotNull { media(it) }
    }

    /**
     * チャンネルマッピング
     */
    fun channels(
        lists: List<NoteList>,
        service: Service
    ): Pageable<Channel> {
        return Pageable<Channel>().also { pg ->
            pg.entities = lists.map { channel(it, service) }
        }
    }

    /**
     * 通知マッピング
     */
    fun notifications(
        notifications: List<MNotification>,
        emojis: List<Emoji>,
        host: String,
        service: Service,
        paging: Paging?
    ): Pageable<Notification> {
        return Pageable<Notification>().also { pg ->
            pg.entities = notifications.map { notification(it, emojis, host, service) }
            pg.paging = MisskeyPaging.fromPaging(paging)
        }
    }

    // ============================================================== //
    // Emojis
    // ============================================================== //
    fun selectEmojis(
        emojis: List<MEmoji>?,
        text: String?,
        host: String
    ): List<Emoji> {
        return if (emojis != null) emojis(emojis)
        else extractEmojis(text, host)
    }

    fun extractEmojis(
        text: String?,
        host: String,
    ): List<Emoji> {

        var t = text
        if (t.isNullOrEmpty()) return listOf()

        val results = mutableListOf<Emoji>()
        val regex = ":([a-zA-Z0-9_]+(@[a-zA-Z0-9-.]+)?):".toRegex()

        while (true) {
            checkNotNull(t)
            val m = regex.find(t) ?: break

            val i = m.range.first
            val found = m.value
            val code = m.value.replace(":", "")
            t = t.substring(i + found.length)

            Emoji().also {
                it.imageUrl = getEmojiURL(host, code)
                it.addShortCode(code)
                results.add(it)
            }
        }

        return results
    }


    /**
     * (from v13)
     * 絵文字の URL を取得
     * code としては以下のような文字列を想定
     * * emoji
     * * emoji@example.com
     * * emoji@.
     */
    private fun getEmojiURL(
        host: String,
        code: String,
    ): String {
        var c = code
        c = c.replace(":", "")
        if (code.endsWith("@.")) {
            c = c.substring(0, c.length - 2)
        }
        return "https://$host/emoji/$c.webp"
    }
}