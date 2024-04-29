package work.socialhub.planetlink.bluesky.action

import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.CommentsRequestImpl
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.TimeLineActionType.UserCommentTimeLine
import work.socialhub.planetlink.define.action.TimeLineActionType.UserLikeTimeLine
import work.socialhub.planetlink.define.action.TimeLineActionType.UserMediaTimeLine
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Request
import work.socialhub.planetlink.model.User

class BlueskyRequest(
    account: Account,
) : RequestActionImpl(account) {

    // ============================================================== //
    // TimeLine API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun localTimeLine(): CommentsRequest =
        throw NotImplementedError()

    /**
     * {@inheritDoc}
     */
    val federationTimeLine: CommentsRequest =
        throw NotImplementedError()

    // ============================================================== //
    // TimeLine API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun homeTimeLine(): CommentsRequest {
        return (super.homeTimeLine() as CommentsRequestImpl).also {
            it.streamFunction = account.action::setHomeTimeLineStream
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userCommentTimeLine(
        id: Identify
    ): CommentsRequest {
        return (super.userCommentTimeLine(id) as CommentsRequestImpl).also {
            if (id is User) setCommentIdentify(it, id.accountIdentify)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userLikeTimeLine(
        id: Identify
    ): CommentsRequest {
        return (super.userLikeTimeLine(id) as CommentsRequestImpl).also {
            if (id is User) setCommentIdentify(it, id.accountIdentify)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun userMediaTimeLine(
        id: Identify
    ): CommentsRequest {
        return (super.userMediaTimeLine(id) as CommentsRequestImpl).also {
            if (id is User) setCommentIdentify(it, id.accountIdentify)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun searchTimeLine(
        query: String
    ): CommentsRequest {
        return (super.searchTimeLine(query) as CommentsRequestImpl).also {
            it.commentFrom().text("$query ")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun messageTimeLine(
        id: Identify
    ): CommentsRequest {
        throw NotImplementedError()
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

            // Comment Mentions
            if (result is CommentsRequest) {
                when (TimeLineActionType.valueOf(request.action)) {
                    UserCommentTimeLine,
                    UserLikeTimeLine,
                    UserMediaTimeLine,
                    -> setCommentIdentify(result, params["to"]!!)

                    else -> {}
                }
            }

            return result
        } catch (e: Exception) {
            println("json parse error (bluesky): ${e.message}.")
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
