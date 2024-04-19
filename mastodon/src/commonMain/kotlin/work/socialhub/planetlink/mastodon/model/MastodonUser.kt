package work.socialhub.planetlink.mastodon.model

import io.ktor.http.*
import work.socialhub.planetlink.micro.MicroBlogUser
import work.socialhub.planetlink.model.Emoji
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.request.CommentForm

/**
 * Mastodon User Model
 * Mastodon のユーザー情報
 */
class MastodonUser(
    service: Service
) : MicroBlogUser(service) {

    /**
     * attributed name (custom emoji included)
     * 絵文字付き属性文字列
     */
    var attributedName: AttributedString? = null
        get() = field ?: let {
            field = AttributedString.plain(name, emptyList())
            field!!.addEmojiElement(emojis)
            field
        }

    /** attributed filed that user input */
    var fields: List<AttributedFiled> = listOf()

    /** emojis which contains in name */
    var emojis: List<Emoji> = listOf()

    /**
     * Get is custom emoji included user.
     * 絵文字付きのユーザー情報かを取得
     */
    val isEmojiIncluded: Boolean
        get() = emojis.isNotEmpty()

    override var name: String = ""
        get() = field.ifEmpty {
            return screenName!!
                .split("@")[0]
                .also { field = it }
        }

    override val accountIdentify: String
        get() {
            // 外のホストの場合は既に ScreenName に URL が付与済
            if (screenName?.contains("@") == true) {
                return "@$screenName"
            }

            // プロフィール URL から HOST を取得
            val host = Url(webUrl).host
            return "@$screenName@$host"
        }

    override var webUrl: String = ""
        get() {
            return field
            // TODO: Pleroma の確認
            /**
            field.ifEmpty {
            val host = accountIdentify.split("@")[2]
            val identify = accountIdentify.split("@")[1]
            return (if (service.isPleroma) {
            "https://$host/$identify"
            } else "https://$host/@$identify")
            .also { field = it }
            }
             */
        }

    override val additionalFields: MutableList<AttributedFiled>
        get() = fields.toMutableList().also {
            it.addAll(additionalFields)
        }

    /**
     * Direct Message Form
     * メッセージフォームは Twitter と Mastodon で扱いが異なる
     * Mastodon の DM はユーザーの AccountIdentify が必要
     */
    override val messageForm: CommentForm
        get() = CommentForm().also {
            it.text = "$accountIdentify "
            it.isMessage = true
        }
}

