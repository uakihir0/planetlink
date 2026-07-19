package work.socialhub.planetlink.x.action

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import work.socialhub.kxweb.model.Media as KMedia
import work.socialhub.kxweb.model.Trend as KTrend
import work.socialhub.kxweb.model.TrendLocation as KTrendLocation
import work.socialhub.kxweb.model.Tweet as KTweet
import work.socialhub.kxweb.model.User as KUser
import work.socialhub.planetlink.define.MediaType
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.Trend
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.x.model.XArticle
import work.socialhub.planetlink.x.model.XComment
import work.socialhub.planetlink.x.model.XPaging
import work.socialhub.planetlink.x.model.XTrendLocation
import work.socialhub.planetlink.x.model.XUser

object XMapper {

    fun user(
        source: KUser,
        service: Service,
    ): XUser {
        return XUser(service).also {
            it.id = ID(checkNotNull(source.id) { "X user ID is missing." })
            it.name = source.name.orEmpty()
            it.screenName = source.screenName
            it.description = AttributedString.plain(source.description)
            it.iconImageUrl = source.profileImageUrl?.replace("_normal.", "_400x400.")
            it.coverImageUrl = source.profileBannerUrl
            it.followersCount = source.followersCount
            it.followingCount = source.followingCount
            it.statusesCount = source.statusesCount
            it.listedCount = source.listedCount
            it.verified = source.verified == true
            it.location = source.location
            it.url = source.url
        }
    }

    fun comment(
        source: KTweet,
        service: Service,
    ): XComment {
        return XComment(service).also {
            it.id = ID(checkNotNull(source.id) { "X post ID is missing." })
            it.text = AttributedString.plain(source.text)
            it.createAt = instant(source.createdAt)
            it.user = source.user?.let { user -> user(user, service) }
            it.medias = source.media.map(::media)
            it.likeCount = source.favoriteCount
            it.shareCount = source.retweetCount
            it.replyCount = source.replyCount
            it.bookmarkCount = source.bookmarkCount
            it.quoteCount = source.quoteCount
            it.viewCount = source.viewCount
            it.conversationId = source.conversationId
            it.language = source.lang
            it.replyTo = source.inReplyToStatusId?.let { id ->
                work.socialhub.planetlink.model.Identify(service, ID(id))
            }
            it.article = source.article?.let { article ->
                XArticle().also { model ->
                    model.id = article.id
                    model.title = article.title
                    model.previewText = article.previewText
                    model.plainText = article.plainText
                    model.coverImageUrl = article.coverImageUrl
                }
            }
        }
    }

    fun timeline(
        sources: List<KTweet>,
        service: Service,
        paging: Paging?,
        nextCursor: String?,
    ): Pageable<Comment> {
        return Pageable<Comment>().also {
            it.entities = sources.map { source -> comment(source, service) }
            it.paging = XPaging.fromPaging(paging).also { page ->
                page.nextCursor = nextCursor
            }
        }
    }

    fun users(
        sources: List<KUser>,
        service: Service,
        paging: Paging?,
        nextCursor: String?,
    ): Pageable<User> {
        return Pageable<User>().also {
            it.entities = sources.map { source -> user(source, service) }
            it.paging = XPaging.fromPaging(paging).also { page ->
                page.nextCursor = nextCursor
            }
        }
    }

    fun trend(source: KTrend): Trend {
        return Trend().also {
            it.name = source.name
            it.query = source.query
            it.volume = source.tweetVolume?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
        }
    }

    fun trendLocation(source: KTrendLocation): XTrendLocation {
        return XTrendLocation().also {
            it.name = source.name
            it.woeid = source.woeid
            it.country = source.country
            it.countryCode = source.countryCode
            it.placeType = source.placeType
            it.parentId = source.parentId
            it.url = source.url
        }
    }

    private fun media(source: KMedia): Media {
        return Media().also {
            it.type = when (source.type?.lowercase()) {
                "photo" -> MediaType.Image
                "video", "animated_gif" -> MediaType.Movie
                else -> MediaType.Other
            }
            it.sourceUrl = source.url
            it.previewUrl = source.url
        }
    }

    private fun instant(value: String?): Instant? {
        if (value == null) return null
        return try {
            val parts = value.split(Regex("\\s+"))
            val time = parts[3].split(":")
            val offset = parts[4]
            val offsetWithColon = "${offset.substring(0, 3)}:${offset.substring(3)}"
            LocalDateTime(
                year = parts[5].toInt(),
                month = month(parts[1]),
                day = parts[2].toInt(),
                hour = time[0].toInt(),
                minute = time[1].toInt(),
                second = time[2].toInt(),
            ).toInstant(UtcOffset.parse(offsetWithColon))
        } catch (_: Exception) {
            null
        }
    }

    private fun month(value: String): Int {
        return when (value) {
            "Jan" -> 1
            "Feb" -> 2
            "Mar" -> 3
            "Apr" -> 4
            "May" -> 5
            "Jun" -> 6
            "Jul" -> 7
            "Aug" -> 8
            "Sep" -> 9
            "Oct" -> 10
            "Nov" -> 11
            "Dec" -> 12
            else -> throw IllegalArgumentException("Unknown month: $value")
        }
    }
}
