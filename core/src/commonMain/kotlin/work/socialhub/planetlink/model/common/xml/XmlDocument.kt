package net.socialhub.planetlink.model.common.xml

import net.socialhub.planetlink.model.common.AttributedElement

/**
 * XML Document
 */
class XmlDocument {
    private var root: XmlTag? = null

    override fun toString(): String {
        return root.toString()
    }

    /**
     * Make XMLDocument to AttributedString
     * XML ドキュメントから属性文字列に変換
     */
    fun toAttributedString(rule: XmlConvertRule): AttributedString {
        val elements: MutableList<AttributedElement?> = java.util.ArrayList<AttributedElement>()
        val text: java.lang.StringBuilder = java.lang.StringBuilder()
        root!!.setAttribute(elements, text, rule)

        // 最後に文字列要素を追加
        if (text.length > 0) {
            val elem: AttributedItem = AttributedItem()
            elem.setDisplayText(text.toString().trimLast())
            elem.setKind(AttributedKind.PLAIN)
            elements.add(elem)
        }

        return AttributedString.elements(elements)
    }

    /**
     * Find Specific name tags.
     * 特定のタグの要素のみを抽出する
     */
    fun findXmlTag(tagName: String?): List<XmlTag> {
        return root!!.findXmlTag(tagName)
    }

    //region // Getter&Setter
    fun getRoot(): XmlTag? {
        return root
    }

    fun setRoot(root: XmlTag?) {
        this.root = root
    } //endregion
}
