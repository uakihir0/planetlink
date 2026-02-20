package work.socialhub.planetlink.misskey.action


import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.CommentsRequestImpl
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.TimeLineActionType.UserCommentTimeLine
import work.socialhub.planetlink.define.action.TimeLineActionType.UserLikeTimeLine
import work.socialhub.planetlink.define.action.TimeLineActionType.UserMediaTimeLine
import work.socialhub.planetlink.misskey.define.MisskeyActionType
import work.socialhub.planetlink.misskey.define.MisskeyActionType.FeaturedTimeline
import work.socialhub.planetlink.misskey.define.MisskeyActionType.FederationTimeLine
import work.socialhub.planetlink.misskey.define.MisskeyActionType.LocalTimeLine
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Request
import work.socialhub.planetlink.model.User
import work.socialhub.planetlink.utils.toBlocking

class MisskeyRequest(
    account: Account
) : RequestActionImpl(account) {

    // ============================================================== //
    // TimeLine API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    val localTimeLine: CommentsRequest
        get() {
            val action = account.action as MisskeyAction
            val request = getCommentsRequest(
                LocalTimeLine,
                action::localTimeLine,
                SerializedRequest(LocalTimeLine)
            )

            request.streamFunction = { cb -> toBlocking { action.localLineStream(cb) } }
            return request
        }

    /**
     * {@inheritDoc}
     */
    val federationTimeLine: CommentsRequest
        get() {
            val action = account.action as MisskeyAction
            val request = getCommentsRequest(
                FederationTimeLine,
                action::federationTimeLine,
                SerializedRequest(FederationTimeLine)
            )

            request.streamFunction = { cb -> toBlocking { action.federationLineStream(cb) } }
            return request
        }

    /**
     * Get Featured Timeline
     * (No Streaming)
     */
    val featuredTimeLine: CommentsRequest
        get() {
            val action = account.action as MisskeyAction
            return getCommentsRequest(
                FeaturedTimeline,
                { paging -> action.featuredTimeLine(paging) },
                SerializedRequest(FeaturedTimeline)
            )
        }

    // ============================================================== //
    // TimeLine API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    val homeTimeLine: CommentsRequest
        get() {
            val action = account.action as MisskeyAction
            return (super.homeTimeLine() as CommentsRequestImpl).also {
                it.streamFunction = { cb -> toBlocking { action.homeTimeLineStream(cb) } }
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
        val request = super.userLikeTimeLine(id) as CommentsRequestImpl
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

            // Misskey
            if (isTypeIncluded(MisskeyActionType.entries, action)) {
                return when (MisskeyActionType.valueOf(action)) {
                    LocalTimeLine -> localTimeLine
                    FederationTimeLine -> federationTimeLine
                    FeaturedTimeline -> featuredTimeLine
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
            println("json parse error (misskey). ${e.message}")
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