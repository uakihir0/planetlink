package net.socialhub.planetlink.action

import work.socialhub.planetlink.action.callback.EventCallback
import work.socialhub.planetlink.define.emoji.EmojiType
import work.socialhub.planetlink.define.emoji.EmojiVariationType
import net.socialhub.planetlink.model.*
import net.socialhub.planetlink.model.error.NotImplimentedException
import net.socialhub.planetlink.model.request.CommentForm
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
    val userMe: User?
        /**
         * Get Authorized My Account
         * 認証した自身のユーザー情報を取得
         */
        get() {
            throw NotImplimentedException()
        }

    /**
     * Get Specific UserInfo
     * 特定のユーザーを取得
     */
    fun getUser(id: Identify?): User {
        throw NotImplimentedException()
    }

    /**
     * Get Specific UserInfo from URL
     * URL からユーザーを取得
     */
    fun getUser(url: String?): User? {
        throw NotImplimentedException()
    }

    /**
     * Follow User
     * ユーザーをフォロー
     */
    fun followUser(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Unfollow User
     * ユーザーをフォロー解除
     */
    fun unfollowUser(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Mute User
     * ユーザーをミュート
     */
    fun muteUser(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Unmute User
     * ユーザーをミュート解除
     */
    fun unmuteUser(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Block User
     * ユーザーをブロック
     */
    fun blockUser(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Unblock User
     * ユーザーをブロック解除
     */
    fun unblockUser(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Get relationship
     * 認証アカウントとの関係を取得
     */
    fun getRelationship(id: Identify?): Relationship {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // User API
    // ユーザー関連 API
    // ============================================================== //
    /**
     * Get Following Account
     * フォローしているユーザー情報を取得
     */
    fun getFollowingUsers(id: Identify?, paging: Paging?): Pageable<User?>? {
        throw NotImplimentedException()
    }

    /**
     * Get Follower Account
     * フォローされているユーザー情報を取得
     */
    fun getFollowerUsers(id: Identify?, paging: Paging?): Pageable<User?>? {
        throw NotImplimentedException()
    }

    /**
     * Search User Account
     * ユーザーアカウントを検索
     */
    fun searchUsers(query: String?, paging: Paging?): Pageable<User?>? {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // TimeLine API
    // タイムライン関連 API
    // ============================================================== //
    /**
     * Get Home TimeLine
     * ホームタイムラインを取得
     */
    fun getHomeTimeLine(paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    /**
     * Get Mention TimeLine
     * メンションタイムラインを取得
     */
    fun getMentionTimeLine(paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    /**
     * Get User Comment TimeLine
     * ユーザーの投稿したコメントのタイムラインを取得
     */
    fun getUserCommentTimeLine(id: Identify?, paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    /**
     * Get User Like TimeLine
     * ユーザーのイイネしたコメントのタイムラインを取得
     */
    fun getUserLikeTimeLine(id: Identify?, paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    /**
     * Get User Media TimeLine
     * ユーザーのメディア一覧を取得
     */
    fun getUserMediaTimeLine(id: Identify?, paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    /**
     * Get Search TimeLine
     * 検索タイムラインを取得
     */
    fun getSearchTimeLine(query: String?, paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // Comment API
    // コメント関連 API
    // ============================================================== //
    /**
     * Post Comment
     * コメントを作成
     */
    fun postComment(req: CommentForm?) {
        throw NotImplimentedException()
    }

    /**
     * Get Comment
     * コメントを取得
     */
    fun getComment(id: Identify?): Comment {
        throw NotImplimentedException()
    }

    /**
     * Get Comment from URL
     * URL からコメントを取得
     */
    fun getComment(url: String?): Comment? {
        throw NotImplimentedException()
    }

    /**
     * Like Comment
     * コメントにたいしてイイねする
     * (Twitter Mastodon ではお気に入りをする)
     */
    fun likeComment(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Unlike Comment
     * コメントに対してのイイねを取り消す
     * (Twitter Mastodon ではお気に入りを消す)
     */
    fun unlikeComment(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Share Comment
     * コメントをシェアする
     */
    fun shareComment(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Unshare Comment
     * コメントのシェアを取り消す
     */
    fun unshareComment(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Reaction Comment
     * リアクションする
     */
    fun reactionComment(id: Identify?, reaction: String?) {
        throw NotImplimentedException()
    }

    /**
     * UnReaction Comment
     * リアクションを取り消す
     */
    fun unreactionComment(id: Identify?, reaction: String?) {
        throw NotImplimentedException()
    }

    /**
     * Delete Comment
     * 自分のコメントを削除
     */
    fun deleteComment(id: Identify?) {
        throw NotImplimentedException()
    }

    /**
     * Get Comment Context
     * コメントについて前後の会話を取得
     */
    fun getCommentContext(id: Identify?): Context {
        throw NotImplimentedException()
    }

    val emojis: List<Emoji?>?
        /**
         * Get Emojis
         * 絵文字一覧を取得
         */
        get() {
            val emojis: MutableList<Emoji> = java.util.ArrayList<Emoji>()

            for (emoji in EmojiType.entries) {
                emojis.add(Emoji.fromEmojiType(emoji))
            }
            for (emoji in EmojiVariationType.entries) {
                emojis.add(Emoji.fromEmojiVariationType(emoji))
            }

            return emojis.stream().sorted(java.util.Comparator<Emoji> { a: Emoji, b: Emoji ->
                val v1 = (if (a.frequentLevel != null) a.frequentLevel else Int.MAX_VALUE)
                val v2 = (if (b.frequentLevel != null) b.frequentLevel else Int.MAX_VALUE)
                v1!!.compareTo(v2!!)
            }).collect<List<Emoji>, Any>(java.util.stream.Collectors.toList<Emoji>())
        }

    // ============================================================== //
    // Channel (List) API
    // チャンネル (リスト) 関連 API
    // ============================================================== //
    /**
     * Get Channels (or Owned Lists)
     * 自分の閲覧可能なチャンネルを取得
     */
    fun getChannels(id: Identify?, paging: Paging?): Pageable<Channel?>? {
        throw NotImplimentedException()
    }

    /**
     * Get Channels Comments
     * チャンネルでの発言を取得
     */
    fun getChannelTimeLine(id: Identify?, paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    /**
     * Get Channels Users
     * チャンネルのユーザーを取得
     */
    fun getChannelUsers(id: Identify?, paging: Paging?): Pageable<User?>? {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // Message API
    // メッセージ関連 API
    // ============================================================== //
    /**
     * Get Message Thread
     * メッセージスレッドを取得
     */
    fun getMessageThread(paging: Paging?): Pageable<Thread?>? {
        throw NotImplimentedException()
    }

    /**
     * Get Message Thread Comments
     * メッセージスレッドの内容を取得
     */
    fun getMessageTimeLine(id: Identify?, paging: Paging?): Pageable<Comment?>? {
        throw NotImplimentedException()
    }

    /**
     * Post Message to Thread
     * メッセージを送信
     */
    fun postMessage(req: CommentForm?) {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // Stream
    // ============================================================== //
    /**
     * Set Home TimeLine Stream
     * ホームタイムラインのストリームイベントを設定
     */
    fun setHomeTimeLineStream(callback: EventCallback?): Stream? {
        throw NotImplimentedException()
    }

    /**
     * Set Notification Stream
     * 通知のストリームイベントを設定
     */
    fun setNotificationStream(callback: EventCallback?): Stream? {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // Request
    // ============================================================== //
    /**
     * Get Request Objects
     * リクエストアクションを取得
     */
    fun request(): RequestAction? {
        throw NotImplimentedException()
    }

    // ============================================================== //
    // Alias
    // エイリアス
    // ============================================================== //
    /**
     * Like <-> Favorite
     */
    fun favoriteComment(id: Identify?) {
        likeComment(id)
    }

    fun unfavoriteComment(id: Identify?) {
        unlikeComment(id)
    }

    /**
     * Share <-> Retweet
     */
    fun retweetComment(id: Identify?) {
        shareComment(id)
    }

    fun unretweetComment(id: Identify?) {
        unshareComment(id)
    }

    /**
     * Channel <-> List
     */
    fun getLists(id: Identify?, paging: Paging?): Pageable<Channel?>? {
        return getChannels(id, paging)
    }
}
