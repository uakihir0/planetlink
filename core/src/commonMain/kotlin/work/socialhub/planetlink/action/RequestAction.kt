package work.socialhub.planetlink.action

import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.UsersRequest
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Request
import kotlin.js.JsExport

@JsExport
interface RequestAction {

    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //
    /**
     * Get Following Account
     * フォローしているユーザー情報を取得
     */
    @Throws(Throwable::class)
    fun followingUsers(id: Identify): UsersRequest

    /**
     * Get Follower Account
     * フォローされているユーザー情報を取得
     */
    @Throws(Throwable::class)
    fun followerUsers(id: Identify): UsersRequest

    /**
     * Search User Account
     * ユーザーアカウントを検索
     */
    @Throws(Throwable::class)
    fun searchUsers(query: String): UsersRequest

    /**
     * Get Channels Users
     * チャンネルのユーザーを取得
     */
    @Throws(Throwable::class)
    fun channelUsers(id: Identify): UsersRequest

    // ============================================================== //
    // TimeLine API
    // タイムライン関連 API
    // ============================================================== //
    /**
     * Get Home TimeLine
     * ホームタイムラインを取得
     */
    @Throws(Throwable::class)
    fun homeTimeLine(): CommentsRequest

    /**
     * Get Mention TimeLine
     * メンションタイムラインを取得
     */
    @Throws(Throwable::class)
    fun mentionTimeLine(): CommentsRequest

    /**
     * Get User Comment TimeLine
     * ユーザーの投稿したコメントのタイムラインを取得
     */
    @Throws(Throwable::class)
    fun userCommentTimeLine(id: Identify): CommentsRequest

    /**
     * Get User Like TimeLine
     * ユーザーのイイネしたコメントのタイムラインを取得
     */
    @Throws(Throwable::class)
    fun userLikeTimeLine(id: Identify): CommentsRequest

    /**
     * Get User Media TimeLine
     * ユーザーのメディア一覧を取得
     */
    @Throws(Throwable::class)
    fun userMediaTimeLine(id: Identify): CommentsRequest

    /**
     * Get Search TimeLine
     * 検索タイムラインを取得
     */
    @Throws(Throwable::class)
    fun searchTimeLine(query: String): CommentsRequest

    /**
     * Get User Bookmark TimeLine
     * ブックマークしたコメントのタイムラインを取得
     */
    @Throws(Throwable::class)
    fun userBookmarkTimeLine(): CommentsRequest

    /**
     * Get Channel TimeLine
     * チャンネルのタイムラインを取得
     */
    @Throws(Throwable::class)
    fun channelTimeLine(id: Identify): CommentsRequest

    /**
     * Get Message TimeLine
     * メッセージのタイムラインを取得
     */
    @Throws(Throwable::class)
    fun messageTimeLine(id: Identify): CommentsRequest

    // ============================================================== //
    // Serialize
    // ============================================================== //
    /**
     * Get Deserialize Request (Comment or User)
     * 文字列からリクエストオブジェクトをリストアする
     */
    @Throws(Throwable::class)
    fun fromRawString(raw: String): Request?
}
