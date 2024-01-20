package work.socialhub.planetlink.action

import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.UsersRequest
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Request

interface RequestAction {

    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //
    /**
     * Get Following Account
     * フォローしているユーザー情報を取得
     */
    fun followingUsers(id: Identify): UsersRequest

    /**
     * Get Follower Account
     * フォローされているユーザー情報を取得
     */
    fun followerUsers(id: Identify): UsersRequest

    /**
     * Search User Account
     * ユーザーアカウントを検索
     */
    fun searchUsers(query: String): UsersRequest

    // ============================================================== //
    // TimeLine API
    // タイムライン関連 API
    // ============================================================== //
    /**
     * Get Home TimeLine
     * ホームタイムラインを取得
     */
    fun homeTimeLine(): CommentsRequest

    /**
     * Get Mention TimeLine
     * メンションタイムラインを取得
     */
    fun mentionTimeLine(): CommentsRequest

    /**
     * Get User Comment TimeLine
     * ユーザーの投稿したコメントのタイムラインを取得
     */
    fun userCommentTimeLine(id: Identify): CommentsRequest

    /**
     * Get User Like TimeLine
     * ユーザーのイイネしたコメントのタイムラインを取得
     */
    fun userLikeTimeLine(id: Identify): CommentsRequest

    /**
     * Get User Media TimeLine
     * ユーザーのメディア一覧を取得
     */
    fun userMediaTimeLine(id: Identify): CommentsRequest

    /**
     * Get Search TimeLine
     * 検索タイムラインを取得
     */
    fun searchTimeLine(query: String): CommentsRequest

    /**
     * Get Channel TimeLine
     * チャンネルのタイムラインを取得
     */
    fun channelTimeLine(id: Identify): CommentsRequest

    /**
     * Get Message TimeLine
     * メッセージのタイムラインを取得
     */
    fun messageTimeLine(id: Identify): CommentsRequest

    // ============================================================== //
    // Serialize
    // ============================================================== //
    /**
     * Get Deserialize Request (Comment or User)
     * 文字列からリクエストオブジェクトをリストアする
     */
    fun fromRawString(raw: String): Request?
}
