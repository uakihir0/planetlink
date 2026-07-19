package work.socialhub.planetlink.model.request

import kotlin.js.JsExport

/**
 * Profile update form.
 * プロフィール更新フォーム。
 *
 * Every field is optional; only the fields that are set are applied.
 */
@JsExport
class ProfileForm {

    /** Display name */
    var displayName: String? = null

    /** Biography / description */
    var description: String? = null

    /** Avatar image bytes */
    var avatar: ByteArray? = null

    /** Avatar image file name */
    var avatarName: String? = null

    /** Banner / header image bytes */
    var banner: ByteArray? = null

    /** Banner / header image file name */
    var bannerName: String? = null
}
