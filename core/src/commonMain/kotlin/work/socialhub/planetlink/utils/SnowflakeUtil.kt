package work.socialhub.planetlink.utils

import kotlinx.datetime.Instant

/**
 * Snowflake IDs
 */
class SnowflakeUtil(
    /**  Snowflake need OriginTime */
    private val originTime: Long
) {

    /**
     * Get Instant from SnowflakeID
     */
    fun getDateTimeFromID(id: Long): Instant {
        val diff = toBinaryString(id, 64)
            .substring(0, 42)
            .toLong(2)

        val mills = (diff + originTime)
        return Instant.fromEpochMilliseconds(mills)
    }

    /**
     * Add MilliSeconds to Snowflake ID
     * (return not existing id (use for paging))
     */
    fun addMilliSecondsToID(id: Long, milliseconds: Long): Long {
        val original = toBinaryString(id, 64)
        val bit = original.substring(0, 42)

        val diff = (bit.toLong(2) + milliseconds)
        val to = toBinaryString(diff, 42)

        val appended = to + original.substring(42, 64)
        return appended.toLong(2)
    }

    /**
     * Add Seconds to Snowflake ID
     * (return not existing id (use for paging))
     */
    fun addSecondsToID(id: Long, seconds: Long): Long {
        return addMilliSecondsToID(id, seconds * 1000L)
    }

    /**
     * Add Minutes to Snowflake ID
     * (return not existing id (use for paging))
     */
    fun addMinutesToID(id: Long, minutes: Long): Long {
        return addSecondsToID(id, minutes * 60L)
    }

    /**
     * Add Hours to Snowflake ID
     * (return not existing id (use for paging))
     */
    fun addHoursToID(id: Long, hours: Long): Long {
        return addMinutesToID(id, hours * 60L)
    }

    /**
     * Add Days to Snowflake ID
     * (return not existing id (use for paging))
     */
    fun addDaysToID(id: Long, days: Long): Long {
        return addHoursToID(id, days * 24L)
    }

    /**
     * バイナリ表現文字列を作成
     */
    private fun toBinaryString(id: Long, length: Int): String {
        var binary = id.toString(2)
        for (i in binary.length..<length) {
            binary = "0${binary}"
        }
        return binary
    }

    companion object {

        /** for X */
        fun ofX(): SnowflakeUtil {
            return SnowflakeUtil(1288834974657L)
        }
    }
}
