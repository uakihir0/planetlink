package work.socialhub.planetlink.x.model

import kotlin.js.JsExport
import work.socialhub.planetlink.micro.MicroBlogUser
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.common.AttributedFiled
import work.socialhub.planetlink.model.common.AttributedString

@JsExport
class XUser(
    service: Service
) : MicroBlogUser(service) {

    var favoritesCount: Int? = null
    var listedCount: Int? = null
    var verified: Boolean = false
    var location: String? = null
    var url: String? = null

    override var name: String = ""
        get() = field.ifEmpty { screenName.orEmpty().also { field = it } }

    override val accountIdentify: String
        get() = "@${screenName.orEmpty()}"

    override var webUrl: String = ""
        get() = field.ifEmpty {
            "https://x.com/${screenName.orEmpty()}".also { field = it }
        }

    override val additionalFields: MutableList<AttributedFiled>
        get() = mutableListOf<AttributedFiled>().also { fields ->
            location?.takeIf { it.isNotBlank() }?.let {
                fields.add(AttributedFiled("Location", AttributedString.plain(it)))
            }
            url?.takeIf { it.isNotBlank() }?.let {
                fields.add(AttributedFiled("URL", AttributedString.plain(it)))
            }
        }
}
