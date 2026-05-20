@file:Suppress("DEPRECATION")
package work.socialhub.planetlink.nostr.action

import kotlinx.datetime.Instant
import work.socialhub.knostr.social.model.NostrNote
import work.socialhub.knostr.social.model.NostrReaction
import work.socialhub.knostr.social.model.NostrRelationship
import work.socialhub.knostr.social.model.NostrUser as KnostrUser
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Reaction
import work.socialhub.planetlink.model.Relationship
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.nostr.model.NostrComment
import work.socialhub.planetlink.nostr.model.NostrPaging
import work.socialhub.planetlink.nostr.model.NostrUser

object NostrMapper {

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

    fun comment(
        note: NostrNote,
        service: Service,
        userMe: User? = null,
    ): NostrComment {
        return NostrComment(service).apply {
            eventId = note.noteId
            id = ID(note.event.id)

            createAt = Instant.fromEpochSeconds(note.createdAt, 0)
            contentWarning = note.contentWarning

            note.author?.let { author ->
                this.user = user(author, service)
            }

            text = AttributedString.plain(note.content)

            replyCount = note.replyCount
            likeCount = note.likeCount
            repostCount = note.repostCount

            reactions = reactions(note.reactions, note.likeCount, note.repostCount, userMe, service)

            note.quotedNote?.let { quoted ->
                sharedComment = comment(quoted, service, userMe)
            }

            medias = medias(note)
        }
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
                type = MediaType.Image
            }
        }
    }

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

    fun relationship(
        rel: NostrRelationship,
    ): Relationship {
        return Relationship().apply {
            following = rel.isFollowing
            followed = rel.isFollowedBy
            muting = rel.isMuting
        }
    }

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
}
