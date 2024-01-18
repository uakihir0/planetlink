package work.socialhub.planetlink.action

import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.CommentsRequestImpl
import work.socialhub.planetlink.action.request.UsersRequest
import work.socialhub.planetlink.action.request.UsersRequestImpl
import work.socialhub.planetlink.define.action.ActionType
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.define.action.UsersActionType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.utils.SerializeUtil

open class RequestActionImpl(
    var account: Account
) : RequestAction {

    // ============================================================== //
    // User API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun followingUsers(
        id: Identify
    ): UsersRequest {
        return getUsersRequest(
            UsersActionType.GetFollowingUsers,
            { paging -> account.action.followingUsers(id, paging) },
            SerializedRequest(UsersActionType.GetFollowingUsers)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun followerUsers(
        id: Identify
    ): UsersRequest {
        return getUsersRequest(
            UsersActionType.GetFollowerUsers,
            { paging -> account.action.followerUsers(id, paging) },
            SerializedRequest(UsersActionType.GetFollowerUsers)
                .add("id", id.id!!.toSerializedString())
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun searchUsers(
        query: String
    ): UsersRequest {
        return getUsersRequest(
            UsersActionType.SearchUsers,
            { paging -> account.action.searchUsers(query, paging) },
            SerializedRequest(UsersActionType.SearchUsers)
                .add("query", query)
        )
    }

    // ============================================================== //
    // TimeLine API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun homeTimeLine(): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.HomeTimeLine,
            { paging -> account.action.homeTimeLine(paging) },
            SerializedRequest(TimeLineActionType.HomeTimeLine)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun mentionTimeLine(): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.MentionTimeLine,
            { paging -> account.action.mentionTimeLine(paging) },
            SerializedRequest(TimeLineActionType.MentionTimeLine)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun userCommentTimeLine(
        id: Identify
    ): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.UserCommentTimeLine,
            { paging -> account.action.userCommentTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.UserCommentTimeLine)
                .add("id", id.id!!.toSerializedString())
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun userLikeTimeLine(
        id: Identify
    ): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.UserLikeTimeLine,
            { paging -> account.action.userLikeTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.UserLikeTimeLine)
                .add("id", id.id!!.toSerializedString())
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun userMediaTimeLine(
        id: Identify
    ): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.UserMediaTimeLine,
            { paging -> account.action.userMediaTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.UserMediaTimeLine)
                .add("id", id.id!!.toSerializedString())
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun searchTimeLine(
        query: String
    ): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.SearchTimeLine,
            { paging -> account.action.searchTimeLine(query, paging) },
            SerializedRequest(TimeLineActionType.SearchTimeLine)
                .add("query", query)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun channelTimeLine(
        id: Identify
    ): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.ChannelTimeLine,
            { paging -> account.action.channelTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.ChannelTimeLine)
                .add("id", id.id!!.toSerializedString())
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun messageTimeLine(
        id: Identify
    ): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.MessageTimeLine,
            { paging -> account.action.messageTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.MessageTimeLine)
                .add("id", id.id!!.toSerializedString())
        ).also {
            it.commentFrom()
                .isMessage(true)
                .replyId(id.id)
        }
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
            val request = SerializeUtil.json.decodeFromString<SerializedRequest>(raw)
            val params = request.params
            val action = request.action

            // Identify
            val id = params["id"]?.let { pid ->
                Identify(account.service).also {
                    it.id = ID.fromSerializedString(pid)
                }
            }

            // Query
            val query = params["query"]

            // User Actions
            if (isTypeIncluded(UsersActionType.entries, action)) {
                return when (UsersActionType.valueOf(action)) {
                    UsersActionType.GetFollowingUsers -> followingUsers(checkNotNull(id))
                    UsersActionType.GetFollowerUsers -> followerUsers(checkNotNull(id))
                    UsersActionType.SearchUsers -> searchTimeLine(checkNotNull(query))
                    UsersActionType.ChannelUsers -> channelTimeLine(checkNotNull(id))
                }.also { it.raw = request }
            }

            // Comment Actions
            if (isTypeIncluded(TimeLineActionType.entries, action)) {
                return when (TimeLineActionType.valueOf(action)) {
                    TimeLineActionType.HomeTimeLine -> homeTimeLine()
                    TimeLineActionType.MentionTimeLine -> mentionTimeLine()
                    TimeLineActionType.SearchTimeLine -> searchTimeLine(checkNotNull(query))
                    TimeLineActionType.ChannelTimeLine -> channelTimeLine(checkNotNull(id))
                    TimeLineActionType.MessageTimeLine -> messageTimeLine(checkNotNull(id))
                    TimeLineActionType.UserLikeTimeLine -> userLikeTimeLine(checkNotNull(id))
                    TimeLineActionType.UserMediaTimeLine -> userMediaTimeLine(checkNotNull(id))
                    TimeLineActionType.UserCommentTimeLine -> userCommentTimeLine(checkNotNull(id))
                }.also { it.raw = request }
            }

            println("invalid action type: $action")
            return null

        } catch (e: Exception) {
            println("json parse error. ${e.message}")
            return null
        }
    }

    private fun isTypeIncluded(
        members: Collection<Enum<*>>,
        action: String
    ): Boolean {
        return members.map { it.name }.contains(action)
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    private fun getUsersRequest(
        type: ActionType,
        usersFunction: (Paging) -> Pageable<User>,
        raw: SerializedRequest
    ): UsersRequestImpl {
        return UsersRequestImpl().also {
            it.usersFunction = usersFunction
            it.actionType = type
            it.account = account
            it.raw = raw
        }
    }

    private fun getCommentsRequest(
        type: ActionType,
        commentsFunction: (Paging) -> Pageable<Comment>,
        raw: SerializedRequest
    ): CommentsRequestImpl {
        return CommentsRequestImpl().also {
            it.commentsFunction = commentsFunction
            it.actionType = type
            it.account = account
            it.raw = raw
        }
    }
}
