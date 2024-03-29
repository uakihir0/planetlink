package work.socialhub.planetlink.misskey.action

import work.socialhub.kmisskey.api.response.i.IResponse
import work.socialhub.kmisskey.entity.Note
import work.socialhub.kmisskey.entity.Notification
import work.socialhub.kmisskey.entity.Relation
import work.socialhub.planetlink.model.*
import work.socialhub.kmisskey.entity.Emoji as KEmoji
import work.socialhub.kmisskey.entity.user.User as KUser

object MisskeyMapper {

    fun user(
        data: IResponse,
        host: String,
        service: Service
    ): User {
        TODO("")
    }

    fun relationship(
        relation: Relation
    ): Relationship {
        TODO("Not yet implemented")
    }

    fun users(
        users: List<KUser>,
        host: String,
        service: Service,
        paging: Paging,
    ): Pageable<User> {
        TODO("")
    }

    fun timeLine(
        notes: List<Note>,
        host: String,
        service: Service,
        paging: Paging
    ): Pageable<Comment> {
        TODO("Not yet implemented")
    }

    fun mentions(
        notes: List<Notification>,
        host: String,
        service: Service,
        paging: Paging,
    ): Pageable<Comment> {
        TODO("Not yet implemented")
    }

    fun comment(
        data: Note,
        host: String,
        service: Service
    ): Comment {
        TODO("Not yet implemented")
    }

    fun emojis(
        emojis: List<KEmoji>
    ): List<Emoji> {
        TODO("Not yet implemented")
    }
}