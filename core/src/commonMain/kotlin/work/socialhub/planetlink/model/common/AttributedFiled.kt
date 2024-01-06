package work.socialhub.planetlink.model.common

/**
 * 追加フィールド
 * Extra Fields
 */
class AttributedFiled {

    var name: String? = null
    var value: AttributedString? = null

    constructor(name: String?, value: AttributedString?) {
        this.value = value
        this.name = name
    }

    constructor(name: String?, value: String?) {
        this.value = AttributedString.plain(value)
        this.name = name
    }
}
