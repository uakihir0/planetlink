package work.socialhub.planetlink.action

import work.socialhub.planetlink.model.Channel
import work.socialhub.planetlink.model.error.NotImplementedException
import net.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.emoji.EmojiType
import work.socialhub.planetlink.define.emoji.EmojiVariationType
import work.socialhub.planetlink.model.*

/**
 * Account Actions
 * (全てのアクションを定義)
 */
interface AccountAction {

    // ============================================================== //
    // Account API
    // アカウント関連 API
    // ============================================================== //
    /**
     * Get Authorized My Account
     * 認証した自身のユーザー情報を取得
     */
    fun userMe(): User {
        throw NotImplementedException()
    }

    /**
     * Get Specific UserInfo
     * 特定のユーザーを取得
     */
    fun user(id: Identify): User {
        throw NotImplementedException()
    }

    /**
     * Get Specific UserInfo from URL
     * URL からユーザーを取得
     */
    fun user(url: String): User {
        throw NotImplementedException()
    }

    /**
     * Follow User
     * ユーザーをフォロー
     */
    fun followUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Unfollow User
     * ユーザーをフォロー解除
     */
    fun unfollowUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Mute User
     * ユーザーをミュート
     */
    fun muteUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Unmute User
     * ユーザーをミュート解除
     */
    fun unmuteUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Block User
     * ユーザーをブロック
     */
    fun blockUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Unblock User
     * ユーザーをブロック解除
     */
    fun unblockUser(id: Identify) {
        throw NotImplementedException()
    }

    /**
     * Get relationship
     * 認証アカウントとの関係を取得
     */
    fun relationship(id: Identify): Relationship {
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
    fun followingUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        throw NotImplementedException()
    }

    /**
     * Get Follower Account
     * フォローされているユーザー情報を取得
     */
    fun followerUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
        throw NotImplementedException()
    }

    /**
     * Search User Account
     * ユーザーアカウントを検索
     */
    fun searchUsers(
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
    fun homeTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get Mention TimeLine
     * メンションタイムラインを取得
     */
    fun mentionTimeLine(
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get User Comment TimeLine
     * ユーザーの投稿したコメントのタイムラインを取得
     */
    fun userCommentTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get User Like TimeLine
     * ユーザーのイイネしたコメントのタイムラインを取得
     */
    fun userLikeTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get User Media TimeLine
     * ユーザーのメディア一覧を取得
     */
    fun userMediaTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get Search TimeLine
     * 検索タイムラインを取得
     */
    fun searchTimeLine(
        query: String,
        paging: Paging,
    ): Pageable<Comment> {
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
    fun postComment(
        req: CommentForm
    ) {
        throw NotImplementedException()
    }

    /**
     * Get Comment
     * コメントを取得
     */
    fun comment(
        id: Identify
    ): Comment {
        throw NotImplementedException()
    }

    /**
     * Get Comment from URL
     * URL からコメントを取得
     */
    fun comment(
        url: String
    ): Comment {
        throw NotImplementedException()
    }

    /**
     * Like Comment
     * コメントにたいしてイイねする
     * (Twitter Mastodon ではお気に入りをする)
     */
    fun likeComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Unlike Comment
     * コメントに対してのイイねを取り消す
     * (Twitter Mastodon ではお気に入りを消す)
     */
    fun unlikeComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Share Comment
     * コメントをシェアする
     */
    fun shareComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Unshare Comment
     * コメントのシェアを取り消す
     */
    fun unshareComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Reaction Comment
     * リアクションする
     */
    fun reactionComment(
        id: Identify,
        reaction: String,
    ) {
        throw NotImplementedException()
    }

    /**
     * UnReaction Comment
     * リアクションを取り消す
     */
    fun unreactionComment(
        id: Identify,
        reaction: String,
    ) {
        throw NotImplementedException()
    }

    /**
     * Delete Comment
     * 自分のコメントを削除
     */
    fun deleteComment(
        id: Identify
    ) {
        throw NotImplementedException()
    }

    /**
     * Get Comment Contexts
     * コメントについて前後の会話を取得
     */
    fun commentContexts(
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
    // Channel (List) API
    // チャンネル (リスト) 関連 API
    // ============================================================== //
    /**
     * Get Channels (or Owned Lists)
     * 自分の閲覧可能なチャンネルを取得
     */
    fun channels(
        id: Identify,
        paging: Paging,
    ): Pageable<Channel> {
        throw NotImplementedException()
    }

    /**
     * Get Channels Comments
     * チャンネルでの発言を取得
     */
    fun channelTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Get Channels Users
     * チャンネルのユーザーを取得
     */
    fun channelUsers(
        id: Identify,
        paging: Paging,
    ): Pageable<User> {
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
    fun messageThread(
        paging: Paging,
    ): Pageable<Thread> {
        throw NotImplementedException()
    }

    /**
     * Get Message Thread Comments
     * メッセージスレッドの内容を取得
     */
    fun messageTimeLine(
        id: Identify,
        paging: Paging,
    ): Pageable<Comment> {
        throw NotImplementedException()
    }

    /**
     * Post Message to Thread
     * メッセージを送信
     */
    fun postMessage(
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
    fun setHomeTimeLineStream(
        callback: EventCallback
    ): Stream {
        throw NotImplementedException()
    }

    /**
     * Set Notification Stream
     * 通知のストリームイベントを設定
     */
    fun setNotificationStream(
        callback: EventCallback
    ): Stream {
        throw NotImplementedException()
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
