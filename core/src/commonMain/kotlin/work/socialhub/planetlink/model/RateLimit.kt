package work.socialhub.planetlink.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import work.socialhub.planetlink.define.action.ActionType

/**
 * SNS レートリミット
 * SNS RateLimit
 */
class RateLimit {

    private val dictionary = mutableMapOf<ActionType, RateLimitValue>()

    /**
     * レートリミット情報を格納
     * Set rate limit info
     */
    fun addInfo(
        action: ActionType,
        service: String,
        limit: Int,
        remaining: Int,
        reset: Instant,
    ) {
        addInfo(
            action,
            RateLimitValue(
                service,
                limit,
                remaining,
                reset
            )
        )
    }

    /**
     * レートリミット情報を格納
     * Set rate limit info
     */
    fun addInfo(
        action: ActionType,
        value: RateLimitValue?
    ) {
        if (value != null) {
            dictionary[action] = value
        }
    }

    /**
     * リクエスト可能かどうか？
     * Is remaining api request count?
     */
    fun isRemaining(
        action: ActionType
    ): Boolean {
        return dictionary.containsKey(action) &&
                dictionary[action]!!.isRemaining()
    }

    class RateLimitValue(
        val service: String,
        val limit: Int,
        val remaining: Int,
        val reset: Instant?
    ) {

        /**
         * リクエスト可能かどうか？
         * Is remaining api request count?
         */
        fun isRemaining(): Boolean {
            val reset = reset ?: return true
            return (remaining > 0) || reset < Clock.System.now()
        }
    }
}
