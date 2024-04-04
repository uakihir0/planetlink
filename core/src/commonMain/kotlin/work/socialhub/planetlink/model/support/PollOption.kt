package work.socialhub.planetlink.model.support

/**
 * Poll's Option
 * 投票における選択肢
 */
class PollOption(
    var index: Int,
    var title: String = "",
    var count: Int = 0,
) {

    /** 認証ユーザーが投票したかどうか？ */
    var isVoted: Boolean = false

    /**
     * 投票の反映
     */
    fun applyVote() {
        isVoted = true
        count++
    }
}
