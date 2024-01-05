package net.socialhub.planetlink.model

import net.socialhub.planetlink.action.AccountAction
import net.socialhub.planetlink.action.UserAction
import net.socialhub.planetlink.action.UserActionImpl
import net.socialhub.planetlink.model.common.AttributedFiled
import net.socialhub.planetlink.model.common.AttributedString
import net.socialhub.planetlink.model.error.NotImplimentedException
import net.socialhub.planetlink.model.request.CommentForm

/**
 * SNS ユーザーモデル
 * SNS User Model
 */
class User(service: Service) : Identify(service) {
    //region // Getter&Setter
    /** User's display name  */
    var name: String? = null

    /**
     * SNS アカウント ID 表現を取得
     * Get SNS Account Identify
     * Need each SNS implementation
     */
    /** User's identified name  */
    var accountIdentify: String? = null
        /**
         * SNS アカウント ID 表現を取得
         * Get SNS Account Identify
         * Need each SNS implementation
         */
        get() = field
        set

    /** User's description  */
    var description: AttributedString? = null

    /** Icon image url  */
    var iconImageUrl: String? = null

    //endregion
    /** Cover image url  */
    var coverImageUrl: String? = null

    /**
     * Get Action
     */
    @Nonnull
    fun action(): UserAction {
        val action: AccountAction = getService().getAccount().action()
        return UserActionImpl(action).user(this)
    }

    val additionalFields: List<Any>
        /**
         * SNS アカウント毎の特殊属性を取得
         * 特定のクラスに変換する事を推奨
         * Get SNS Additional Fields
         * (recommend to cast specified SNS model)
         */
        get() = java.util.ArrayList<AttributedFiled>()

    val commentForm: CommentForm
        /**
         * Get Comment Form
         * コメント投稿用のフォームを取得
         */
        get() {
            throw NotImplimentedException()
        }

    val messageForm: CommentForm
        /**
         * Get Message Form
         * メッセージ投稿用のフォームを取得
         */
        get() {
            throw NotImplimentedException()
        }

    val webUrl: String
        /**
         * Get Web Url
         * Web のアドレスを取得
         */
        get() {
            throw NotImplimentedException()
        }
}
