package work.socialhub.planetlink.model

import kotlinx.datetime.Instant
import net.socialhub.planetlink.model.support.PollOption

/**
 * Poll
 * 投票情報
 */
class Poll(
    service: Service
) : Identify(service) {

    /** 有効期限 (時刻)  */
    var expireAt: Instant? = null

    /** 有効期限切れしたかどうか？  */
    var isExpired: Boolean = false

    // endregion
    /** 複数投票が可能か？  */
    var isMultiple: Boolean = false

    /** 投票数  */
    var votesCount: Long? = null

    /** 投票人数  */
    var votersCount: Long? = null

    /** 認証ユーザーが投票したかどうか？  */
    var isVoted: Boolean = false

    /** 投票選択肢  */
    var options: List<PollOption>? = null

    /**
     * 投票の反映
     */
    fun applyVote(choices: List<Int>?) {
        if (choices.isNullOrEmpty()) {
            return
        }

        isVoted = true
        votersCount?.let {
            votersCount = it + 1
        }
        votesCount?.let {
            votesCount = it + choices.size
        }

        choices.forEach { i ->
            if (options!!.size > i) {
                options!![i].applyVote()
            }
        }
    }
}
