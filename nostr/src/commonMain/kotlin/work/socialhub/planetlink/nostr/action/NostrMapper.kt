package work.socialhub.planetlink.nostr.action

import kotlin.time.Instant
import work.socialhub.knostr.social.model.NostrChannel as KnostrChannel
import work.socialhub.knostr.social.model.NostrChannelMessage
import work.socialhub.knostr.social.model.NostrMediaUpload
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.social.model.NostrReaction
import work.socialhub.knostr.social.model.NostrRelationship
import work.socialhub.knostr.social.model.NostrThread
import work.socialhub.knostr.social.model.NostrUser as KnostrUser
import work.socialhub.knostr.util.Nip21
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Context
import work.socialhub.planetlink.model.Emoji
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedItem
import work.socialhub.planetlink.model.common.AttributedKind
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.request.MediaForm
import work.socialhub.planetlink.nostr.model.NostrComment
import work.socialhub.planetlink.nostr.model.NostrPaging
import work.socialhub.planetlink.nostr.model.NostrUser

/** Nostr エンティティのマッピング */
object NostrMapper {

    private val NOSTR_EVENT_REFERENCE =
        Regex("nostr:(?:note|nevent)1[ac-hj-np-z02-9]+", RegexOption.IGNORE_CASE)

    private val NOSTR_EMOJI_SHORTCODE = Regex("[A-Za-z0-9_]{1,64}")

    /** ユーザーマッピング */
    fun user(
        knostrUser: KnostrUser,
        service: Service,
    ): NostrUser {
        return NostrUser(service).apply {
            id = ID(knostrUser.pubkey)
            name = knostrUser.displayName ?: knostrUser.name ?: knostrUser.pubkey.take(8)
            npub = knostrUser.npub
            nip05 = knostrUser.nip05
            lud16 = knostrUser.lud16
            displayName = knostrUser.displayName ?: knostrUser.name

            iconImageUrl = knostrUser.picture
            coverImageUrl = knostrUser.banner

            if (!knostrUser.about.isNullOrEmpty()) {
                description = AttributedString.plain(knostrUser.about)
            }

            followingCount = knostrUser.followingCount
            followersCount = knostrUser.followersCount
        }
    }

    fun toNostrMediaUpload(form: MediaForm): NostrMediaUpload {
        return NostrMediaUpload(
            fileData = form.data,
            fileName = form.name,
            mimeType = nostrMediaMimeType(form.name),
            description = form.description.orEmpty(),
        )
    }

    fun nostrMediaMimeType(fileName: String?): String {
        return when (fileName?.substringAfterLast('.', "")?.lowercase()) {
            "avif" -> "image/avif"
            "bmp" -> "image/bmp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "jpe", "jpeg", "jpg" -> "image/jpeg"
            "png" -> "image/png"
            "svg" -> "image/svg+xml"
            "tif", "tiff" -> "image/tiff"
            "webp" -> "image/webp"
            "m4v" -> "video/x-m4v"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "oga", "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    /** コメントマッピング */
    fun comment(
        note: NostrNote,
        service: Service,
        userMe: User? = null,
    ): NostrComment {
        return NostrComment(service).apply {
            eventId = note.noteId
            quotedEventId = note.quotedEventId
            id = ID(note.event.id)

            createAt = Instant.fromEpochSeconds(note.createdAt, 0)
            contentWarning = note.contentWarning

            this.authorPubkey = note.event.pubkey
            note.author?.let { author ->
                this.user = user(author, service)
            }

            text = attributedText(note)

            replyCount = note.replyCount
            likeCount = note.likeCount
            repostCount = note.repostCount
            possiblySensitive = note.isSensitive

            reactions = reactions(note.reactions, note.likeCount, note.repostCount, userMe, service)

            note.quotedNote?.let { quoted ->
                sharedComment = comment(quoted, service, userMe)
            }

            medias = medias(note)
        }
    }

    internal fun commentContext(
        thread: NostrThread,
        service: Service,
        userMe: User? = null,
    ): Context {
        val targetId = thread.rootNote?.event?.id
        val notes = thread.replies.distinctBy { it.event.id }
        val notesById = notes.associateBy { it.event.id }
        val ancestorIds = mutableSetOf<String>()

        var current = thread.rootNote
        while (current != null) {
            val parentId = replyParentId(current) ?: break
            if (!ancestorIds.add(parentId)) break
            current = notesById[parentId] ?: break
        }

        return Context().also { context ->
            context.ancestors = notes
                .filter { it.event.id != targetId && it.event.id in ancestorIds }
                .map { comment(it, service, userMe) }
            context.descendants = notes
                .filter { it.event.id != targetId && it.event.id !in ancestorIds }
                .map { comment(it, service, userMe) }
            context.sort()
        }
    }

    private fun replyParentId(note: NostrNote): String? {
        val eventTags = note.event.tags.filter { it.size >= 2 && it[0] == "e" }
        if (eventTags.isEmpty()) return null

        return eventTags.firstOrNull { it.size >= 4 && it[3] == "reply" }?.get(1)
            ?: eventTags.firstOrNull { it.size >= 4 && it[3] == "root" }?.get(1)
            ?: if (eventTags.size == 1) eventTags[0][1] else eventTags.last()[1]
    }

    private fun reactions(
        reactions: List<NostrReaction>,
        likeCount: Int,
        repostCount: Int,
        userMe: User?,
        service: Service,
    ): List<Reaction> {
        val models = mutableListOf<Reaction>()

        if (likeCount > 0 || reactions.any { it.content == "+" }) {
            val likeReactions = reactions.filter { it.content == "+" }
            val model = Reaction()
            model.count = maxOf(likeCount, likeReactions.size)
            model.name = "like"
            model.reacting = likeReactions.any {
                it.author?.pubkey == userMe?.id?.value<String>()
            }
            models.add(model)
        }

        val emojiReactions = reactions.filter { it.content != "+" && it.content != "-" }
        emojiReactions.groupBy { it.content }.forEach { (content, group) ->
            val model = Reaction()
            model.count = group.size
            model.name = content
            model.reacting = group.any {
                it.author?.pubkey == userMe?.id?.value<String>()
            }
            group.firstOrNull()?.emojiUrl?.let { model.iconUrl = it }
            models.add(model)
        }

        if (repostCount > 0) {
            val model = Reaction()
            model.count = repostCount
            model.name = "repost"
            models.add(model)
        }

        return models
    }

    private fun medias(note: NostrNote): List<Media> {
        return note.medias.map { media ->
            Media().apply {
                sourceUrl = media.url
                previewUrl = media.thumbnailUrl ?: media.url
                type = mediaType(media.mimeType, media.url)
                width = media.width
                height = media.height
                description = media.alt
                blurhash = media.blurhash
            }
        }
    }

    private fun mediaType(mimeType: String?, url: String): MediaType {
        val normalizedMimeType = mimeType?.lowercase()
        return when {
            normalizedMimeType?.startsWith("image/") == true -> MediaType.Image
            normalizedMimeType?.startsWith("video/") == true -> MediaType.Movie
            normalizedMimeType?.startsWith("audio/") == true -> MediaType.Audio
            normalizedMimeType != null && normalizedMimeType != "application/octet-stream" -> MediaType.File
            else -> when (url.substringBefore('?').substringAfterLast('.', "").lowercase()) {
                "avif", "bmp", "gif", "heic", "heif", "jpeg", "jpg", "png", "svg", "tif", "tiff", "webp" ->
                    MediaType.Image
                "m4v", "mkv", "mov", "mp4", "webm" -> MediaType.Movie
                "aac", "flac", "m4a", "mp3", "oga", "ogg", "wav" -> MediaType.Audio
                else -> MediaType.File
            }
        }
    }

    fun channel(
        source: KnostrChannel,
        service: Service,
    ): Channel {
        return Channel(service).apply {
            id = ID(source.id)
            name = source.name
            description = source.about
            createAt = Instant.fromEpochSeconds(source.createdAt)
            isPublic = true
        }
    }

    fun channels(
        sources: List<KnostrChannel>,
        service: Service,
        paging: Paging,
    ): Pageable<Channel> {
        return Pageable<Channel>().apply {
            entities = sources.map { channel(it, service) }
            this.paging = NostrPaging.fromPaging(paging)
        }
    }

    fun channelMessages(
        messages: List<NostrChannelMessage>,
        users: Map<String, KnostrUser>,
        service: Service,
        paging: Paging,
    ): Pageable<Comment> {
        return Pageable<Comment>().apply {
            entities = messages
                .sortedByDescending { it.createdAt }
                .map { message ->
                    NostrComment(service).apply {
                        id = ID(message.event.id)
                        eventId = message.event.id
                        channelId = message.channelId
                        authorPubkey = message.event.pubkey
                        createAt = Instant.fromEpochSeconds(message.createdAt)
                        text = AttributedString.plain(message.content)
                        users[message.event.pubkey]?.let {
                            user = user(it, service)
                        }
                    }
                }
            this.paging = NostrPaging.fromPaging(paging)
        }
    }

    /** タイムラインマッピング */
    fun timeLine(
        notes: List<NostrNote>,
        service: Service,
        paging: Paging?,
        userMe: User? = null,
    ): Pageable<Comment> {
        val model = Pageable<Comment>()
        model.entities = notes.map { note ->
            comment(note, service, userMe)
        }

        model.paging = NostrPaging.fromPaging(paging)
        return model
    }

    /** リレーションシップマッピング */
    fun relationship(
        rel: NostrRelationship,
    ): Relationship {
        return Relationship().apply {
            following = rel.isFollowing
            followed = rel.isFollowedBy
            muting = rel.isMuting
        }
    }

    /** ユーザーリストをページング可能なリストに変換 */
    fun usersToPageable(
        users: List<KnostrUser>,
        service: Service,
        paging: Paging?,
    ): Pageable<User> {
        val model = Pageable<User>()
        model.entities = users.map { user(it, service) }
        model.paging = NostrPaging.fromPaging(paging)
        return model
    }

    private fun attributedText(note: NostrNote): AttributedString {
        val eventHashtags = note.event.tags
            .filter { it.size >= 2 && it[0] == "t" }
            .map { it[1].lowercase() }
            .toSet()

        val attributed = AttributedString.plain(displayContent(note))

        val validated = attributed.elements.map { elem ->
            if (elem.kind == AttributedKind.HASH_TAG && elem is AttributedItem) {
                val tagText = elem.displayText.removePrefix("#").removePrefix("＃").lowercase()
                if (tagText in eventHashtags) elem
                else AttributedItem().also {
                    it.kind = AttributedKind.PLAIN
                    it.displayText = elem.displayText
                    it.expandedText = elem.displayText
                }
            } else elem
        }
        return AttributedString(validated).also {
            it.addEmojiElement(extractEmojis(note))
        }
    }

    internal fun extractEmojis(note: NostrNote): List<Emoji> {
        val shortCodes = mutableSetOf<String>()
        var processed = 0
        return note.event.tags.mapNotNull { tag ->
            if (tag.size < 3 || tag[0] != "emoji") return@mapNotNull null
            if (processed >= 64) return@mapNotNull null

            val shortCode = tag[1]
            val imageUrl = tag[2]
            if (!NOSTR_EMOJI_SHORTCODE.matches(shortCode) ||
                imageUrl.isBlank() ||
                !shortCodes.add(shortCode)
            ) {
                return@mapNotNull null
            }
            processed++

            Emoji().also {
                it.addShortCode(shortCode)
                it.imageUrl = imageUrl
            }
        }
    }

    private fun displayContent(note: NostrNote): String {
        val quotedEventId = note.quotedNote?.event?.id ?: return note.content
        return stripQuoteReference(note.content, quotedEventId)
    }

    internal fun stripQuotePreservingAttributes(
        text: AttributedString,
        quotedEventId: String,
    ): AttributedString {
        val updatedElements = text.elements.mapNotNull { elem ->
            if (elem.kind == AttributedKind.PLAIN) {
                val stripped = stripQuoteReference(elem.displayText, quotedEventId)
                if (stripped.isEmpty()) null
                else AttributedItem().also {
                    it.kind = AttributedKind.PLAIN
                    it.displayText = stripped
                    it.expandedText = stripped
                }
            } else {
                elem
            }
        }
        return AttributedString.elements(updatedElements)
    }

    internal fun stripQuoteReference(
        content: String,
        quotedEventId: String,
    ): String {
        val stripped = NOSTR_EVENT_REFERENCE.replace(content) { match ->
            val referencedEventId = Nip21.extractEventIds(match.value.lowercase()).singleOrNull()
            if (referencedEventId.equals(quotedEventId, ignoreCase = true)) {
                ""
            } else {
                match.value
            }
        }
        return if (stripped == content) content else stripped.trimEnd()
    }
}
