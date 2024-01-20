package work.socialhub.planetlink.model

import work.socialhub.planetlink.action.UserActionImpl
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.error.NotImplementedException
import work.socialhub.planetlink.model.request.CommentForm

/**
 * SNS ユーザーモデル
 * SNS User Model
 */
open class User(
    service: Service
) : Identify(service) {

    /** User's display name  */
    open var name: String? = null

    /**
     * SNS アカウント ID 表現を取得
     * Get SNS Account Identify
     * Need each SNS implementation
     */
    open val accountIdentify: String
        get() = throw NotImplementedException()

    /** User's description  */
    var description: AttributedString? = null

    /** Icon image url  */
    var iconImageUrl: String? = null

    /** Cover image url  */
    var coverImageUrl: String? = null

    /** Get Action */
    val action by lazy {
        val action = service.account.action
        UserActionImpl(action, this)
    }

    /**
     * SNS アカウント毎の特殊属性を取得
     * 特定のクラスに変換する事を推奨
     * Get SNS Additional Fields
     * (recommend to cast specified SNS model)
     */
    val additionalFields = mutableListOf<AttributedFiled>()

    /**
     * Get Comment Form
     * コメント投稿用のフォームを取得
     */
    val commentForm: CommentForm
        get() = throw NotImplementedException()

    /**
     * Get Message Form
     * メッセージ投稿用のフォームを取得
     */
    val messageForm: CommentForm
        get() = throw NotImplementedException()

    /**
     * Get Web Url
     * Web のアドレスを取得
     */
    open val webUrl: String
        get() = throw NotImplementedException()
}
