package net.socialhub.planetlink.model.support

/**
 * Poll's Option
 * 投票における選択肢
 */
class PollOption {
    // region
    var index: Long? = null

    var title: String? = null

    var count: Long? = null

    // endregion
    /** 認証ユーザーが投票したかどうか？  */
    var isVoted: Boolean = false

    /**
     * 投票の反映
     */
    fun applyVote() {
        isVoted = true
        if (count == null) {
            count = 0L
        }
        count = count!! + 1
    }
}
