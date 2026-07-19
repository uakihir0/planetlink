package work.socialhub.planetlink.action

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.emoji.EmojiType
import work.socialhub.planetlink.define.emoji.EmojiVariationType
import work.socialhub.planetlink.model.*
import work.socialhub.planetlink.model.error.NotImplementedException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.request.ProfileForm
import kotlin.js.JsExport

/**
 * Account Actions
 * (全てのアクションを定義)
 */
@JsExport
interface AccountAction {

    // ============================================================== //
    // Account API
    // アカウント関連 API
    // ============================================================== //
    /**
     * Get Authorized My Account
     * 認証した自身のユーザー情報を取得
     */
    suspend fun userMe(): User {
        throw NotImplementedException()
    }

    /**
     * Get Specific UserInfo
     * 特定のユーザーを取得
     */
    suspend fun user(id: Identify): User {
        throw NotImplementedException()
    }

    /**
     * Get Specific UserInfo from URL
     * URL からユーザーを取得
     */
    @JsExport.Ignore
    suspend fun user(url: String): User {
        throw NotImplementedException()
    }

    /**
     * Follow User
     * ユーザーをフォロー
     */
    suspend fun followUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Unfollow User
     * ユーザーをフォロー解除
     */
    suspend fun unfollowUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Mute User
     * ユーザーをミュート
     */
    suspend fun muteUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Unmute User
     * ユーザーをミュート解除
     */
    suspend fun unmuteUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Block User
     * ユーザーをブロック
     */
    suspend fun blockUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Unblock User
     * ユーザーをブロック解除
     */
    suspend fun unblockUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Get relationship
     * 認証アカウントとの関係を取得
     */
    suspend fun relationship(id: Identify): Relationship {
        throw NotImplementedException()
    }

    /**
     * Accept Follow Request
     * フォローリクエストを承認 (id = 申請者)
     */
    suspend fun acceptFollowRequest(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Reject Follow Request
     * フォローリクエストを拒否 (id = 申請者)
     */
    suspend fun rejectFollowRequest(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Report User
     * ユーザーを通報
     */
    suspend fun reportUser(
        id: Identify,
        comment: String? = null,
    ) {
        throw NotImplementedException()
    }

    /**
     * Update Profile
     * 自身のプロフィールを更新
     */
    suspend fun updateProfile(form: ProfileForm) {
        throw NotImplementedException()
    }

    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //
    /**
     * Get Following Account
     * フォローしているユーザー情報を取得
     */
    suspend fun followingUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        throw NotImplementedException()
    }

    /**
     * Get Follower Account
     * フォローされているユーザー情報を取得
     */
    suspend fun followerUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        throw NotImplementedException()
    }

    /**
     * Search User Account
     * ユーザーアカウントを検索
     */
    suspend fun searchUsers(
        query: String,
        paging: Paging,
    ): Pageable<User> {
        throw NotImplementedException()
    }

    // ============================================================== //
    // TimeLine API
    // タイムライン関連 API
    // ============================================================== //
    /**
     * Get Home TimeLine
     * ホームタイムラインを取得
     */
    suspend fun homeTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get Mention TimeLine
     * メンションタイムラインを取得
     */
    suspend fun mentionTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get User Comment TimeLine
     * ユーザーの投稿したコメントのタイムラインを取得
     */
    suspend fun userCommentTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get User Like TimeLine
     * ユーザーのイイネしたコメントのタイムラインを取得
     */
    suspend fun userLikeTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get User Media TimeLine
     * ユーザーのメディア一覧を取得
     */
    suspend fun userMediaTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get Search TimeLine
     * 検索タイムラインを取得
     */
    suspend fun searchTimeLine(
        query: String,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get User Bookmark TimeLine
     * ブックマークしたコメントのタイムラインを取得
     */
    suspend fun userBookmarkTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    // ============================================================== //
    // Notification API
    // 通知関連 API
    // ============================================================== //
    /**
     * Get Notifications
     * 通知一覧を取得
     */
    suspend fun notification(
        paging: Paging,
    ): Pageable<Notification> {
        throw NotImplementedException()
    }

    /**
     * Mark Notifications as Read
     * 通知を既読にする (upToId 指定時はそこまで、未指定時は全件)
     */
    suspend fun markNotificationsRead(
        upToId: Identify? = null,
    ) {
        throw NotImplementedException()
    }

    // ============================================================== //
    // Comment API
    // コメント関連 API
    // ============================================================== //
    /**
     * Post Comment
     * コメントを作成
     */
    suspend fun postComment(
        req: CommentForm
    ) {
        throw NotImplementedException()
    }

    /**
     * Get Comment
     * コメントを取得
     */
    suspend fun comment(
        id: Identify
    ): Comment {
        throw NotImplementedException()
    }

    /**
     * Get Comment from URL
     * URL からコメントを取得
     */
    @JsExport.Ignore
    suspend fun comment(
        url: String
    ): Comment {
        throw NotImplementedException()
    }

    /**
     * Like Comment
     * コメントにたいしてイイねする
     * (Twitter Mastodon ではお気に入りをする)
     */
    suspend fun likeComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Unlike Comment
     * コメントに対してのイイねを取り消す
     * (Twitter Mastodon ではお気に入りを消す)
     */
    suspend fun unlikeComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Share Comment
     * コメントをシェアする
     */
    suspend fun shareComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Unshare Comment
     * コメントのシェアを取り消す
     */
    suspend fun unshareComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Reaction Comment
     * リアクションする
     */
    suspend fun reactionComment(
        id: Identify,
        reaction: String,
    ) {
        throw NotImplementedException()
    }

    /**
     * UnReaction Comment
     * リアクションを取り消す
     */
    suspend fun unreactionComment(
        id: Identify,
        reaction: String,
    ) {
        throw NotImplementedException()
    }

    /**
     * Delete Comment
     * 自分のコメントを削除
     */
    suspend fun deleteComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Edit Comment
     * 自分のコメントを編集
     */
    suspend fun editComment(
        id: Identify,
        req: CommentForm,
    ) {
        throw NotImplementedException()
    }

    /**
     * Report Comment
     * コメントを通報
     */
    suspend fun reportComment(
        id: Identify,
        comment: String? = null,
    ) {
        throw NotImplementedException()
    }

    /**
     * Bookmark Comment
     * コメントをブックマーク
     */
    suspend fun bookmarkComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Unbookmark Comment
     * コメントのブックマークを解除
     */
    suspend fun unbookmarkComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Vote Poll
     * 投票する
     */
    suspend fun votePoll(
        id: Identify,
        choices: List<Int>
    ) {
        throw NotImplementedException()
    }

    /**
     * Get Comment Contexts
     * コメントについて前後の会話を取得
     */
    suspend fun commentContexts(
        id: Identify
    ): Context {
        throw NotImplementedException()
    }

    /**
     * Get Emojis
     * 絵文字一覧を取得
     */
    fun emojis(): List<Emoji> {
        val emojis = mutableListOf<Emoji>()

        for (emoji in EmojiType.entries) {
            emojis.add(Emoji.fromEmojiType(emoji))
        }
        for (emoji in EmojiVariationType.entries) {
            emojis.add(Emoji.fromEmojiVariationType(emoji))
        }

        return emojis.sortedBy { it.frequentLevel ?: Int.MAX_VALUE }
    }

    // ============================================================== //
    // Space / Channel (List) API
    // スペース / チャンネル (リスト) 関連 API
    // ============================================================== //
    /**
     * Get Spaces (top-level containers)
     * スペース (上位コンテナ) の一覧を取得
     *
     * A Space is the container that holds channels: a Discord guild, a Slack
     * workspace, a Matrix space, etc. Pass one of the returned Spaces to
     * [channels] to fetch that Space's channels only.
     */
    suspend fun spaces(
        paging: Paging,
    ): Pageable<Space> {
        throw NotImplementedException()
    }

    /**
     * Get Channels (or Owned Lists)
     * 自分の閲覧可能なチャンネルを取得
     *
     * For Space-based platforms (Discord, ...), [id] is a Space id and only
     * that Space's channels are returned. For list-based platforms
     * (Mastodon / Misskey / Slack) [id] keeps its existing meaning
     * (the authenticated user / a single workspace).
     */
    suspend fun channels(
        id: Identify,
        paging: Paging,
    ): Pageable<Channel> {
        throw NotImplementedException()
    }

    /**
     * Get Channels Comments
     * チャンネルでの発言を取得
     */
    suspend fun channelTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get Channels Users
     * チャンネルのユーザーを取得
     */
    suspend fun channelUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        throw NotImplementedException()
    }

    /**
     * Create List (Channel)
     * リスト (チャンネル) を作成
     */
    suspend fun createList(
        name: String,
        description: String? = null,
    ): Channel {
        throw NotImplementedException()
    }

    /**
     * Add User to List (Channel)
     * リスト (チャンネル) にユーザーを追加
     */
    suspend fun addUserToList(
        channel: Identify,
        user: Identify,
    ) {
        throw NotImplementedException()
    }

    /**
     * Remove User from List (Channel)
     * リスト (チャンネル) からユーザーを削除
     */
    suspend fun removeUserFromList(
        channel: Identify,
        user: Identify,
    ) {
        throw NotImplementedException()
    }

    // ============================================================== //
    // Message API
    // メッセージ関連 API
    // ============================================================== //
    /**
     * Get Message Thread
     * メッセージスレッドを取得
     */
    suspend fun messageThread(
        paging: Paging,
    ): Pageable<Thread> {
        throw NotImplementedException()
    }

    /**
     * Get Message Thread Comments
     * メッセージスレッドの内容を取得
     */
    suspend fun messageTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Post Message to Thread
     * メッセージを送信
     */
    suspend fun postMessage(
        req: CommentForm
    ) {
        throw NotImplementedException()
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * Set Home TimeLine Stream
     * ホームタイムラインのストリームイベントを設定
     */
    suspend fun setHomeTimeLineStream(
        callback: EventCallback
    ): Stream {
        throw NotImplementedException()
    }

    /**
     * Set Notification Stream
     * 通知のストリームイベントを設定
     */
    suspend fun setNotificationStream(
        callback: EventCallback
    ): Stream {
        throw NotImplementedException()
    }

    // ============================================================== //
    // Capabilities
    // ============================================================== //
    /**
     * Get Capabilities
     * このアダプターがサポートする機能一覧を取得
     */
    fun capabilities(): Capabilities {
        return Capabilities(emptySet())
    }

    // ============================================================== //
    // Request
    // ============================================================== //
    /**
     * Get Request Objects
     * リクエストアクションを取得
     */
    fun request(): RequestAction {
        throw NotImplementedException()
    }
}
