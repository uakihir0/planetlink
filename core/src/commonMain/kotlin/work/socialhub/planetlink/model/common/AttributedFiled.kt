package work.socialhub.planetlink.model.common

import kotlin.js.JsExport

/**
 * 追加フィールド
 * Extra Fields
 */
@JsExport
class AttributedFiled {

    var name: String? = null
    var value: AttributedString? = null

    @JsExport.Ignore
    constructor(name: String?, value: AttributedString?) {
        this.value = value
        this.name = name
    }

    @JsExport.Ignore
    constructor(name: String?, value: String?) {
        this.value = AttributedString.plain(value)
        this.name = name
    }
}
