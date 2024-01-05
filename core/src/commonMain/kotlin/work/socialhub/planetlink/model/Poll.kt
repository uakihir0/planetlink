package net.socialhub.planetlink.model

import net.socialhub.planetlink.model.support.PollOption

/**
 * Poll
 * 投票情報
 */
class Poll(service: Service) : Identify(service) {
    // region
    /** 有効期限 (時刻)  */
    var expireAt: java.util.Date? = null

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
    private var options: List<PollOption>? = null

    /**
     * 投票の反映
     */
    fun applyVote(choices: List<Int?>?) {
        if (choices == null || choices.size == 0) {
            return
        }

        isVoted = true
        if (votersCount != null) {
            votersCount = votersCount!! + 1
        }
        if (votesCount != null) {
            votesCount = votesCount!! + choices.size.toLong()
        }
        choices.forEach(java.util.function.Consumer<Int> { i: Int ->
            if (options!!.size > i) {
                options!![i].applyVote()
            }
        })
    }

    fun getOptions(): List<PollOption>? {
        return options
    }

    fun setOptions(options: List<PollOption>?) {
        this.options = options
    }
}
