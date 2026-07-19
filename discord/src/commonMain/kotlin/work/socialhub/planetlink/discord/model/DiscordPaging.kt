package work.socialhub.planetlink.discord.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import kotlin.js.JsExport

/**
 * Discord paging using snowflake `before`/`after` cursors.
 *
 * Discord returns channel messages newest-first. Fetching newer messages uses
 * the `after` cursor (the newest known id); fetching older messages uses the
 * `before` cursor (the oldest known id). Snowflakes are kept as strings to
 * avoid precision loss.
 */
@JsExport
class DiscordPaging : Paging() {

    /** Fetch messages before this id (older). */
    var before: String? = null

    /** Fetch messages after this id (newer). */
    var after: String? = null

    override fun <T : Identify> newPage(entities: List<T>): Paging {
        val newPage = DiscordPaging()
        newPage.count = count
        if (entities.isNotEmpty()) {
            // Newest entity id -> fetch messages after it.
            newPage.after = entities.first().id?.value<String>()
        }
        return newPage
    }

    override fun <T : Identify> pastPage(entities: List<T>): Paging {
        val pastPage = DiscordPaging()
        pastPage.count = count
        if (entities.isNotEmpty()) {
            // Oldest entity id -> fetch messages before it.
            pastPage.before = entities.last().id?.value<String>()
        }
        return pastPage
    }

    override fun setMarkPagingEnd(entities: List<*>) {
        val pageSize = count ?: return
        if (isHasPast
            && pageSize > 0
            && entities.size < pageSize
            && after == null
        ) {
            isHasPast = false
        }
    }

    override fun copy(): DiscordPaging {
        val p = DiscordPaging()
        copyTo(p)
        p.before = before
        p.after = after
        return p
    }

    companion object {
        fun fromPaging(paging: Paging?): DiscordPaging {
            if (paging is DiscordPaging) {
                return paging.copy()
            }
            val p = DiscordPaging()
            if (paging != null) {
                paging.copyTo(p)
            }
            return p
        }
    }
}
