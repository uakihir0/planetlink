package net.socialhub.planetlink.model.common

/**
 * 追加フィールド
 * Extra Fields
 */
class AttributedFiled {
    //region // Getter&Setter
    var name: String? = null
    private var value: AttributedString? = null

    constructor()

    constructor(name: String?, value: AttributedString?) {
        this.value = value
        this.name = name
    }

    constructor(name: String?, value: String?) {
        this.value = AttributedString.plain(value)
        this.name = name
    }

    fun getValue(): AttributedString? {
        return value
    }

    fun setValue(value: AttributedString?) {
        this.value = value
    } //endregion
}
