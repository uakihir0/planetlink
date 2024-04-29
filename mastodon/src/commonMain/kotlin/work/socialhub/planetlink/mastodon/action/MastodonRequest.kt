package work.socialhub.planetlink.mastodon.action

import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.CommentsRequestImpl
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.TimeLineActionType.MessageTimeLine
import work.socialhub.planetlink.define.action.TimeLineActionType.UserCommentTimeLine
import work.socialhub.planetlink.define.action.TimeLineActionType.UserLikeTimeLine
import work.socialhub.planetlink.define.action.TimeLineActionType.UserMediaTimeLine
import work.socialhub.planetlink.mastodon.define.MastodonActionType
import work.socialhub.planetlink.mastodon.define.MastodonActionType.FederationTimeLine
import work.socialhub.planetlink.mastodon.define.MastodonActionType.LocalTimeLine
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Request
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.request.CommentForm

class MastodonRequest(
    account: Account
) : RequestActionImpl(account) {

    // ============================================================== //
    // Mastodon TimeLine API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    val localTimeLine: CommentsRequest
        get() {
            val action = account.action as MastodonAction
            val request = getCommentsRequest(
                LocalTimeLine,
                action::localTimeLine,
                SerializedRequest(LocalTimeLine)
            )

            request.streamFunction = action::localLineStream
            return request
        }

    /**
     * {@inheritDoc}
     */
    val federationTimeLine: CommentsRequest
        get() {
            val action = account.action as MastodonAction
            val request = getCommentsRequest(
                FederationTimeLine,
                action::federationTimeLine,
                SerializedRequest(FederationTimeLine)
            )

            request.streamFunction = action::federationLineStream
            return request
        }

    // ============================================================== //
    // TimeLine API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    val homeTimeLine: CommentsRequest
        get() {
            val action = account.action as MastodonAction
            return (super.homeTimeLine() as CommentsRequestImpl).also {
                it.streamFunction = action::homeTimeLineStream
            }
        }

    /**
     * {@inheritDoc}
     */
    override fun userCommentTimeLine(
        id: Identify
    ): CommentsRequest {
        val request = super.userCommentTimeLine(id) as CommentsRequestImpl
        if (id is User) {
            setCommentIdentify(request, id.accountIdentify)
        }
        return request
    }

    /**
     * {@inheritDoc}
     */
    override fun userLikeTimeLine(
        id: Identify
    ): CommentsRequest {
        val request: CommentsRequestImpl = super.userLikeTimeLine(id) as CommentsRequestImpl
        if (id is User) {
            setCommentIdentify(request, id.accountIdentify)
        }
        return request
    }

    /**
     * {@inheritDoc}
     */
    override fun userMediaTimeLine(
        id: Identify
    ): CommentsRequest {
        val request: CommentsRequestImpl = super.userMediaTimeLine(id) as CommentsRequestImpl
        if (id is User) {
            setCommentIdentify(request, id.accountIdentify)
        }
        return request
    }

    /**
     * {@inheritDoc}
     */
    override fun searchTimeLine(
        query: String
    ): CommentsRequest {
        val request = super.searchTimeLine(query) as CommentsRequestImpl
        request.commentForm!!.text("$query ")
        return request
    }

    /**
     * {@inheritDoc}
     */
    override fun messageTimeLine(
        id: Identify
    ): CommentsRequest {
        val request = CommentsRequestImpl()
        request.actionType = MessageTimeLine
        request.account = account
        request.commentForm = CommentForm()
            .also { it.isMessage = true }

        request.commentsFunction = { paging ->
            account.action.messageTimeLine(id, paging).also { pg ->
                // 最新の投稿の ID を取得してコメント対象に設定
                val max = pg.entities.maxBy { it.createAt!!.toEpochMilliseconds() }
                request.commentForm!!.replyId = max.id
            }
        }

        request.raw = SerializedRequest(MessageTimeLine)
            .add("id", id.id!!.toSerializedString())
        return request
    }

    // ============================================================== //
    // From Serialized
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun fromRawString(
        raw: String
    ): Request? {
        try {
            val result = super.fromRawString(raw) ?: return null
            val request = checkNotNull(result.raw)
            val params = request.params
            val action = request.action

            // Mastodon
            if (isTypeIncluded(MastodonActionType.entries, action)) {
                return when (MastodonActionType.valueOf(action)) {
                    LocalTimeLine -> localTimeLine
                    FederationTimeLine -> federationTimeLine
                }
            }

            // Comment Mentions
            if (result is CommentsRequest) {
                when (TimeLineActionType.valueOf(action)) {
                    UserCommentTimeLine,
                    UserLikeTimeLine,
                    UserMediaTimeLine
                    -> setCommentIdentify(result, params["to"]!!)

                    else -> {}
                }
            }
            return result

        } catch (e: Exception) {
            println("json parse error (mastodon). ${e.message}")
            return null
        }
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    private fun setCommentIdentify(
        request: CommentsRequest,
        identify: String
    ) {
        request.commentFrom().text("$identify ")
        request.raw!!.add("to", identify)
    }
}
