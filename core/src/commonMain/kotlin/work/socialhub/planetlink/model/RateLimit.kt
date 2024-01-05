package net.socialhub.planetlink.model

import net.socialhub.planetlink.define.ServiceType
import net.socialhub.planetlink.define.action.ActionType

/**
 * SNS レートリミット
 * SNS RateLimit
 */
class RateLimit : java.io.Serializable {
    private val dictionary: MutableMap<ActionType?, RateLimitValue> = java.util.HashMap<ActionType, RateLimitValue>()

    /**
     * レートリミット情報を格納
     * Set rate limit info
     */
    fun addInfo(
        action: ActionType?,
        service: ServiceType?,
        limit: Int,
        remaining: Int,
        reset: java.util.Date
    ) {
        val value = RateLimitValue(
            service, limit, remaining, reset
        )
        addInfo(action, value)
    }

    /**
     * レートリミット情報を格納
     * Set rate limit info
     */
    fun addInfo(
        action: ActionType?,
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
    fun isRemaining(action: ActionType?): Boolean {
        return dictionary.containsKey(action) &&  //
                dictionary[action]!!.isRemaining()
    }

    class RateLimitValue(
        service: ServiceType?,
        val limit: Int,
        private val remaining: Int,
        reset: java.util.Date
    ) {
        private val service: ServiceType? = service

        private val reset: java.util.Date = reset

        /**
         * リクエスト可能かどうか？
         * Is remaining api request count?
         */
        fun isRemaining(): Boolean {
            return (remaining > 0) || reset.before(java.util.Date())
        }

        // region
        fun getService(): ServiceType? {
            return service
        }

        fun getRemaining(): Int {
            return remaining
        }

        fun getReset(): java.util.Date {
            return reset
        } // endregion
    }
}
