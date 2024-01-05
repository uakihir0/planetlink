package net.socialhub.planetlink.action

import net.socialhub.planetlink.action.request.CommentsRequest
import net.socialhub.planetlink.model.Request

interface RequestAction {
    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //
    /**
     * Get Following Account
     * フォローしているユーザー情報を取得
     */
    fun getFollowingUsers(id: Identify?): UsersRequest?

    /**
     * Get Follower Account
     * フォローされているユーザー情報を取得
     */
    fun getFollowerUsers(id: Identify?): UsersRequest?

    /**
     * Search User Account
     * ユーザーアカウントを検索
     */
    fun searchUsers(query: String?): UsersRequest?

    // ============================================================== //
    // TimeLine API
    // タイムライン関連 API
    // ============================================================== //
    /**
     * Get Home TimeLine
     * ホームタイムラインを取得
     */
    val homeTimeLine: CommentsRequest?

    /**
     * Get Mention TimeLine
     * メンションタイムラインを取得
     */
    val mentionTimeLine: CommentsRequest?

    /**
     * Get User Comment TimeLine
     * ユーザーの投稿したコメントのタイムラインを取得
     */
    fun getUserCommentTimeLine(id: Identify?): CommentsRequest?

    /**
     * Get User Like TimeLine
     * ユーザーのイイネしたコメントのタイムラインを取得
     */
    fun getUserLikeTimeLine(id: Identify?): CommentsRequest?

    /**
     * Get User Media TimeLine
     * ユーザーのメディア一覧を取得
     */
    fun getUserMediaTimeLine(id: Identify?): CommentsRequest?

    /**
     * Get Search TimeLine
     * 検索タイムラインを取得
     */
    fun getSearchTimeLine(query: String?): CommentsRequest?

    /**
     * Get Channel TimeLine
     * チャンネルのタイムラインを取得
     */
    fun getChannelTimeLine(id: Identify?): CommentsRequest?

    /**
     * Get Message TimeLine
     * メッセージのタイムラインを取得
     */
    fun getMessageTimeLine(id: Identify?): CommentsRequest?

    // ============================================================== //
    // Serialize
    // ============================================================== //
    /**
     * Get Deserialize Request (Comment or User)
     * 文字列からリクエストオブジェクトをリストアする
     */
    fun fromSerializedString(serialize: String?): Request?
}
