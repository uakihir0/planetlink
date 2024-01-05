package net.socialhub.planetlink.action

import com.google.gson.Gson
import work.socialhub.planetlink.model.Pageable
import net.socialhub.planetlink.model.Request
import net.socialhub.planetlink.model.User
import work.socialhub.planetlink.model.Comment

class RequestActionImpl(account: Account?) : RequestAction {
    private val log: Logger = Logger.getLogger(RequestActionImpl::class.java)

    protected var account: Account?

    init {
        this.account = account
    }

    // ============================================================== //
    // User API
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun getFollowingUsers(id: Identify?): UsersRequest {
        return getUsersRequest(
            UsersActionType.GetFollowingUsers,
            java.util.function.Function<Paging, Pageable<User>> { paging: Paging? ->
                account.action().getFollowingUsers(id, paging)
            },
            SerializeBuilder(UsersActionType.GetFollowingUsers)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun getFollowerUsers(id: Identify?): UsersRequest {
        return getUsersRequest(
            UsersActionType.GetFollowerUsers,
            java.util.function.Function<Paging, Pageable<User>> { paging: Paging? ->
                account.action().getFollowerUsers(id, paging)
            },
            SerializeBuilder(UsersActionType.GetFollowerUsers)
                .add("id", id.serializedIdString)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun searchUsers(query: String?): UsersRequest {
        return getUsersRequest(
            UsersActionType.SearchUsers,
            java.util.function.Function<Paging, Pageable<User>> { paging: Paging? ->
                account.action().searchUsers(query, paging)
            },
            SerializeBuilder(UsersActionType.SearchUsers)
                .add("query", query)
        )
    }

    // ============================================================== //
    // TimeLine API
    // ============================================================== //
    override val homeTimeLine: CommentsRequest
        /**
         * {@inheritDoc}
         */
        get() = getCommentsRequest(
            TimeLineActionType.HomeTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getHomeTimeLine(paging)
            },
            SerializeBuilder(TimeLineActionType.HomeTimeLine)
        )

    override val mentionTimeLine: CommentsRequest
        /**
         * {@inheritDoc}
         */
        get() = getCommentsRequest(
            TimeLineActionType.MentionTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getMentionTimeLine(paging)
            },
            SerializeBuilder(TimeLineActionType.MentionTimeLine)
        )

    /**
     * {@inheritDoc}
     */
    override fun getUserCommentTimeLine(id: Identify?): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.UserCommentTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getUserCommentTimeLine(id, paging)
            },
            SerializeBuilder(TimeLineActionType.UserCommentTimeLine)
                .add("id", id.serializedIdString)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun getUserLikeTimeLine(id: Identify?): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.UserLikeTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getUserLikeTimeLine(id, paging)
            },
            SerializeBuilder(TimeLineActionType.UserLikeTimeLine)
                .add("id", id.serializedIdString)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun getUserMediaTimeLine(id: Identify?): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.UserMediaTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getUserMediaTimeLine(id, paging)
            },
            SerializeBuilder(TimeLineActionType.UserMediaTimeLine)
                .add("id", id.serializedIdString)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun getSearchTimeLine(query: String?): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.SearchTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getSearchTimeLine(query, paging)
            },
            SerializeBuilder(TimeLineActionType.SearchTimeLine)
                .add("query", query)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun getChannelTimeLine(id: Identify?): CommentsRequest {
        return getCommentsRequest(
            TimeLineActionType.ChannelTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getChannelTimeLine(id, paging)
            },
            SerializeBuilder(TimeLineActionType.ChannelTimeLine)
                .add("id", id.serializedIdString)
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun getMessageTimeLine(id: Identify?): CommentsRequest {
        val request: CommentsRequest = getCommentsRequest(
            TimeLineActionType.MessageTimeLine,
            java.util.function.Function<Paging, Pageable<Comment>> { paging: Paging? ->
                account.action().getMessageTimeLine(id, paging)
            },
            SerializeBuilder(TimeLineActionType.MessageTimeLine)
                .add("id", id.serializedIdString)
        )

        request.getCommentFrom()
            .message(true)
            .replyId(id)
        return request
    }

    // ============================================================== //
    // From Serialized
    // ============================================================== //
    /**
     * {@inheritDoc}
     */
    override fun fromSerializedString(serialize: String?): Request? {
        try {
            val params: SerializeParams = Gson().fromJson(serialize, SerializeParams::class.java)
            val action = params.get("action")

            // Identify
            var id: Identify? = null
            if (params.contains("id")) {
                id = Identify(account.service)
                id.serializedIdString = params.get("id")
            }

            // Query
            var query: String? = null
            if (params.contains("query")) {
                query = params.get("query")
            }

            // ------------------------------------------------------------- //
            // User Actions
            // ------------------------------------------------------------- //
            if (isTypeIncluded(UsersActionType.entries.toTypedArray(), action)) {
                when (UsersActionType.valueOf(action)) {
                    UsersActionType.GetFollowingUsers -> return getFollowingUsers(id)
                    UsersActionType.GetFollowerUsers -> return getFollowerUsers(id)
                    UsersActionType.SearchUsers -> return getSearchTimeLine(query)
                    UsersActionType.ChannelUsers -> return getChannelTimeLine(id)

                    else -> {
                        log.debug("invalid user action type: $action")
                        return null
                    }
                }
            }

            // ------------------------------------------------------------- //
            // Comment Actions
            // ------------------------------------------------------------- //
            if (isTypeIncluded(TimeLineActionType.entries.toTypedArray(), action)) {
                when (TimeLineActionType.valueOf(action)) {
                    TimeLineActionType.HomeTimeLine -> return homeTimeLine
                    TimeLineActionType.MentionTimeLine -> return mentionTimeLine
                    TimeLineActionType.SearchTimeLine -> return getSearchTimeLine(query)
                    TimeLineActionType.ChannelTimeLine -> return getChannelTimeLine(id)
                    TimeLineActionType.MessageTimeLine -> return getMessageTimeLine(id)
                    TimeLineActionType.UserLikeTimeLine -> return getUserLikeTimeLine(id)
                    TimeLineActionType.UserMediaTimeLine -> return getUserMediaTimeLine(id)
                    TimeLineActionType.UserCommentTimeLine -> return getUserCommentTimeLine(id)

                    else -> {
                        log.debug("invalid comment action type: $action")
                        return null
                    }
                }
            }

            log.debug("invalid action type: $action")
            return null
        } catch (e: java.lang.Exception) {
            log.debug("json parse error.", e)
            return null
        }
    }

    protected fun isTypeIncluded(members: Array<Enum<*>?>, action: String?): Boolean {
        val names: List<String?> = java.util.stream.Stream.of<Enum<*>>(*members)
            .map<String>(java.util.function.Function<Enum<*>, String> { obj: Enum<*> -> obj.name })
            .collect<List<String>, Any>(java.util.stream.Collectors.toList<String>())
        return names.contains(action)
    }

    // ============================================================== //
    // Inner Class
    // ============================================================== //
    /**
     * Serialize Params
     */
    class SerializeParams {
        private val params: MutableMap<String, String?> = java.util.HashMap<String, String>()

        fun get(key: String): String? {
            return params[key]
        }

        fun contains(key: String): Boolean {
            return params.containsKey(key)
        }

        fun add(key: String, value: String?) {
            params[key] = value
        }
    }

    /**
     * Serialize Builder
     */
    class SerializeBuilder(action: Enum<T?>) {
        private val params = SerializeParams()

        init {
            add("action", action.name)
        }

        fun add(key: String, value: String?): SerializeBuilder {
            params.add(key, value)
            return this
        }

        fun toJson(): String {
            return Gson().toJson(params)
        }
    }

    // ============================================================== //
    // Support
    // ============================================================== //
    // User
    protected fun getUsersRequest(
        type: ActionType?,
        usersFunction: java.util.function.Function<Paging?, Pageable<User?>?>?,
        serializeBuilder: SerializeBuilder?
    ): UsersRequestImpl {
        val request: UsersRequestImpl = UsersRequestImpl()
        request.setSerializeBuilder(serializeBuilder)
        request.setUsersFunction(usersFunction)
        request.setActionType(type)
        request.setAccount(account)
        return request
    }

    // Comments
    protected fun getCommentsRequest(
        type: ActionType?,
        commentsFunction: java.util.function.Function<Paging?, Pageable<Comment?>?>?,
        serializeBuilder: SerializeBuilder?
    ): CommentsRequestImpl {
        val request: CommentsRequestImpl = CommentsRequestImpl()
        request.setSerializeBuilder(serializeBuilder)
        request.setCommentsFunction(commentsFunction)
        request.setActionType(type)
        request.setAccount(account)
        return request
    }

    //region // Getter&Setter
    fun getAccount(): Account? {
        return account
    }

    fun setAccount(account: Account?) {
        this.account = account
    } //endregion
}
