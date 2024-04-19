package work.socialhub.planetlink.mastodon.action

import work.socialhub.kmastodon.api.response.Response
import work.socialhub.kmastodon.api.response.ResponseUnit
import work.socialhub.kmastodon.entity.Account
import work.socialhub.kmastodon.entity.Status
import work.socialhub.kmastodon.entity.share.Link
import work.socialhub.planetlink.model.*
import work.socialhub.kmastodon.entity.Relationship as MRelationship

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
}