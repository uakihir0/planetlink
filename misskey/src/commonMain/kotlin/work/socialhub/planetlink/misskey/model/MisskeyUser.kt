package work.socialhub.planetlink.misskey.model

import work.socialhub.planetlink.micro.MicroBlogUser
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Emoji
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString
import work.socialhub.planetlink.model.support.Color

/**
 * Misskey User
 * Misskey のユーザー情報
 */
class MisskeyUser(
    service: Service
) : MicroBlogUser(service) {

    /** Is simple (UserLite) object?  */
    var isSimple: Boolean = false

    /** attributed filed that user input  */
    var fields: List<AttributedFiled> = listOf()

    /** user pinned owned comments */
    var pinnedComments: List<Comment> = listOf()

    /** emojis which contains in name */
    var emojis: List<Emoji> = listOf()

    /** User setting location */
    var location: String? = null

    /** Host account belong to */
    var host: String? = null

    var isCat: Boolean = false
    var isBot: Boolean = false

    /** Color of users */
    var avatarColor: Color? = null
    var bannerColor: Color? = null

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

    /**
     * Get is custom emoji included user.
     * 絵文字付きのユーザー情報かを取得
     */
    val isEmojiIncluded: Boolean
        get() = emojis.isNotEmpty()

    override var name: String = ""
        get() {
            return field.ifEmpty {
                return checkNotNull(screenName)
                    .split("@")[0]
                    .also { field = it }
            }
        }

    override val accountIdentify: String
        get() = ("@$screenName@$host")

    override var webUrl: String = ""
        get() = field.ifEmpty {
            val host = accountIdentify.split("@")[2]
            val identify = accountIdentify.split("@")[1]
            return "https://$host/@$identify"
                .also { field = it }
        }
}