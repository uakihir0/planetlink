package work.socialhub.planetlink.model

import net.socialhub.planetlink.action.UserAction
import net.socialhub.planetlink.action.UserActionImpl
import work.socialhub.planetlink.model.common.AttributedString
import net.socialhub.planetlink.model.error.NotImplimentedException
import net.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.model.common.AttributedFiled

/**
 * SNS ユーザーモデル
 * SNS User Model
 */
class User(service: Service) : Identify(service) {

    /** User's display name  */
    var name: String? = null

    /**
     * SNS アカウント ID 表現を取得
     * Get SNS Account Identify
     * Need each SNS implementation
     */
    /** User's identified name  */
    var accountIdentify: String? = null

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
    fun action(): UserAction {
        val action = service.account.action
        return UserActionImpl(action).user(this)
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
        get() = throw NotImplimentedException()

    /**
     * Get Message Form
     * メッセージ投稿用のフォームを取得
     */
    val messageForm: CommentForm
        get() = throw NotImplimentedException()

    /**
     * Get Web Url
     * Web のアドレスを取得
     */
    val webUrl: String
        get() = throw NotImplimentedException()
}
