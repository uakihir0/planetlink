package work.socialhub.planetlink.bluesky.model

import work.socialhub.planetlink.micro.MicroBlogUser
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString
import kotlin.js.JsExport

/**
 * Bluesky User Model
 * Bluesky のユーザー情報
 */
@JsExport
class BlueskyUser(
    service: Service
) : MicroBlogUser(service) {

    /**
     * Is simple User object?
     * 簡易ユーザー情報か？
     * (一部の情報が抜けています)
     */
    var isSimple: Boolean = false

    /**
     * Follow Record Uri
     * フォローのレコードの URI
     * (フォローしている場合のみ記録される)
     */
    var followRecordUri: String? = null

    var followedRecordUri: String? = null

    var blockingRecordUri: String? = null

    var muted: Boolean? = null

    var blockedBy: Boolean? = null

    /** Free-form pronouns set on the profile. */
    var pronouns: String? = null

    /** Deprecated website field of the profile record. */
    var website: String? = null

    /** Links shown on the profile, in display order. */
    var profileLinks: List<BlueskyProfileLink> = listOf()

    override var name: String = ""
        get() = field.ifEmpty {
            return screenName.orEmpty()
                .also { field = it }
        }

    override val accountIdentify
        get() = "@$screenName"

    // TODO: HOST名変更
    override var webUrl: String = ""
        get() = field.ifEmpty {
            return "https://bsky.app/profile/$screenName"
                .also { field = it }
        }

    override val additionalFields: MutableList<AttributedFiled>
        get() = mutableListOf<AttributedFiled>().also { fields ->
            pronouns?.takeIf { it.isNotBlank() }?.let {
                fields.add(AttributedFiled("Pronouns", AttributedString.plain(it)))
            }
            website?.takeIf { it.isNotBlank() }?.let {
                fields.add(AttributedFiled("Website", AttributedString.plain(it)))
            }
            profileLinks.forEach { link ->
                fields.add(
                    AttributedFiled(
                        link.title?.takeIf { it.isNotBlank() } ?: "Link",
                        AttributedString.plain(link.url),
                    )
                )
            }
        }
}
