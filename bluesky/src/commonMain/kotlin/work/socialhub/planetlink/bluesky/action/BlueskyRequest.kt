package work.socialhub.planetlink.bluesky.action

import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.model.Account

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
    val homeTimeLine: CommentsRequest
        /**
         * {@inheritDoc}
         */
        get() {
            val request: CommentsRequestImpl = super.getHomeTimeLine() as CommentsRequestImpl

            request.setStreamFunction(account.action()::setHomeTimeLineStream)
            return request
        }

    /**
     * {@inheritDoc}
     */
    fun getUserCommentTimeLine(id: Identify): CommentsRequest {
        val request: CommentsRequestImpl = super.getUserCommentTimeLine(id) as CommentsRequestImpl

        if (id is User) {
            setCommentIdentify(request, (id as User).getAccountIdentify())
        }
        return request
    }

    /**
     * {@inheritDoc}
     */
    fun getUserLikeTimeLine(id: Identify): CommentsRequest {
        val request: CommentsRequestImpl = super.getUserLikeTimeLine(id) as CommentsRequestImpl

        if (id is User) {
            setCommentIdentify(request, (id as User).getAccountIdentify())
        }
        return request
    }

    /**
     * {@inheritDoc}
     */
    fun getUserMediaTimeLine(id: Identify): CommentsRequest {
        val request: CommentsRequestImpl = super.getUserMediaTimeLine(id) as CommentsRequestImpl

        if (id is User) {
            setCommentIdentify(request, (id as User).getAccountIdentify())
        }
        return request
    }

    /**
     * {@inheritDoc}
     */
    fun getSearchTimeLine(query: String): CommentsRequest {
        val request: CommentsRequestImpl = super.getSearchTimeLine(query) as CommentsRequestImpl
        request.getCommentFrom().text("$query ")
        return request
    }

    /**
     * {@inheritDoc}
     */
    fun getMessageTimeLine(id: Identify?): CommentsRequest {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // From Serialized
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    fun fromSerializedString(serialize: String?): Request? {
        try {
            val params: SerializeParams = Gson().fromJson(serialize, SerializeParams::class.java)
            val action: String = params.get("action")

            val result: Request = super.fromSerializedString(serialize)

            // Comment Mentions
            if (result is CommentsRequest) {
                val req: CommentsRequest = result as CommentsRequest
                when (TimeLineActionType.valueOf(action)) {
                    UserCommentTimeLine, UserLikeTimeLine, UserMediaTimeLine -> setCommentIdentify(
                        req,
                        params.get("to")
                    )

                    else -> {}
                }
            }

            return result
        } catch (e: java.lang.Exception) {
            log.debug("json parse error (mastodon).", e)
            return null
        }
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    private fun setCommentIdentify(request: CommentsRequest, identify: String) {
        request.getSerializeBuilder().add("to", identify)
        request.getCommentFrom().text("$identify ")
    }
}
