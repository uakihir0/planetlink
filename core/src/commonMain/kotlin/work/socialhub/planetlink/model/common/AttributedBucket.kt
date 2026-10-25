package work.socialhub.planetlink.model.common

import kotlin.js.JsExport


@JsExport
class AttributedBucket : AttributedElement {

    override var kind = AttributedKind.OTHER

    override var visible: Boolean = true

    private val params = mutableMapOf<String, String>()

    /** 入れ子にする要素（引用の中身など） */
    val children = mutableListOf<AttributedElement>()

    /** 入れ子の要素を追加する */
    fun add(element: AttributedElement): AttributedBucket = also {
        children.add(element)
    }

    override val displayText: String
        get() {
            if (!visible) {
                return ""
            }
            return children
                .map { it.displayText }
                .joinToString("")
        }

    override val expandedText: String?
        get() = null
}
