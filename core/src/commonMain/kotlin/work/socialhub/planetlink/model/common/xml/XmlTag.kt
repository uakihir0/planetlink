package net.socialhub.planetlink.model.common.xml

import net.socialhub.planetlink.model.common.AttributedBucket

class XmlTag : XmlElement {
    //region // Getter&Setter
    var name: String? = null

    private var elements: List<XmlElement> = java.util.ArrayList<XmlElement>()

    var attributes: Map<String, String> = java.util.HashMap<String, String>()

    fun setAttribute(
        elems: MutableList<AttributedElement?>,
        builder: java.lang.StringBuilder,
        rule: XmlConvertRule
    ) {
        // 見えない要素の場合は無視

        if (attributes.containsKey("class")) {
            if (attributes["class"] == "invisible") {
                return
            }
        }

        // ------------------------------------------------------------------- //
        // <BR>: 改行の場合は改行
        if (name.equals("br", ignoreCase = true)) {
            builder.append(rule.getBr())
            return
        }

        // ------------------------------------------------------------------- //
        // <P>: タグの後は段落
        if (name.equals("p", ignoreCase = true)) {
            expandAttribute(elems, builder, rule)
            builder.append(rule.getP())
            return
        }


        // ------------------------------------------------------------------- //
        // <A>: リンクの処理
        if (name.equals("a", ignoreCase = true)) {
            expandStringElement(elems, builder)
            expandAttribute(elems, builder, rule)

            val displayText: String = builder.toString()
            builder.setLength(0)

            if (attributes.containsKey("class")) {
                // ハッシュタグの場合

                if (attributes["class"]!!.contains("hashtag")) {
                    val elem: AttributedItem = AttributedItem()
                    elem.setKind(AttributedKind.HASH_TAG)
                    elem.setDisplayText(displayText)
                    elems.add(elem)
                    return
                }

                // ユーザー向け URL の場合 (Mastodon)
                if (attributes["class"]!!.contains("u-url")) {
                    val elem: AttributedItem = AttributedItem()
                    val href = attributes["href"]
                    elem.setKind(AttributedKind.ACCOUNT)
                    elem.setDisplayText(displayText)
                    elem.setExpandedText(href)
                    elems.add(elem)
                    return
                }
            }

            val elem: AttributedItem = AttributedItem()
            elem.setKind(AttributedKind.LINK)
            elem.setExpandedText(attributes["href"])
            elem.setDisplayText(displayText)
            elems.add(elem)
            return
        }

        // ------------------------------------------------------------------- //
        // <BLOCKQUOTE>: 引用の処理 (Tumblr)
        if (name.equals("blockquote", ignoreCase = true)) {
            expandStringElement(elems, builder)

            val elem: AttributedBucket = AttributedBucket()
            elem.setChildren(java.util.ArrayList<E>())
            elem.setKind(AttributedKind.QUOTE)
            elems.add(elem)

            // 再帰的に中身を走査
            val text: java.lang.StringBuilder = java.lang.StringBuilder()
            expandAttribute(elem.getChildren(), text, rule)

            // 最後に文字列要素を追加
            if (text.length > 0) {
                val item: AttributedItem = AttributedItem()
                item.setDisplayText(text.toString().trimLast())
                item.setKind(AttributedKind.PLAIN)
                elem.getChildren().add(item)
            }
            return
        }


        // TODO: テキスト装飾系


        // その他の場合は無視して続行
        expandAttribute(elems, builder, rule)
    }

    /** 文字を切り出す処理  */
    private fun expandStringElement(
        elems: MutableList<AttributedElement?>,
        builder: java.lang.StringBuilder
    ) {
        // 空文字の場合は処理を行わない

        if (builder.length > 0) {
            val elem: AttributedItem = AttributedItem()
            elem.setDisplayText(builder.toString())
            elem.setKind(AttributedKind.PLAIN)
            elems.add(elem)

            // Clear Buffer
            builder.setLength(0)
        }
    }

    private fun expandAttribute(
        elems: List<AttributedElement?>,
        builder: java.lang.StringBuilder,
        rule: XmlConvertRule
    ) {
        for (element in elements) {
            element.setAttribute(elems, builder, rule)
        }
    }

    private fun getHost(url: String): String {
        try {
            return java.net.URL(url).getHost()
        } catch (e: java.lang.Exception) {
            throw SocialHubException(e)
        }
    }

    /**
     * Find Specific name tags.
     * 特定のタグの要素のみを抽出する
     */
    fun findXmlTag(tagName: String?): List<XmlTag> {
        val tags: java.util.ArrayList<XmlTag> = java.util.ArrayList<XmlTag>()

        // 対象タグが画像の場合はそのタグを加える
        if (name.equals(tagName, ignoreCase = true)) {
            tags.add(this)
        }

        // XmlTag の場合は分岐して処理
        for (elem in elements) {
            if (elem is XmlTag) {
                tags.addAll(elem.findXmlTag(tagName))
            }
        }

        return tags
    }

    override fun toString(): String {
        // 見えない要素の場合は空文字

        if (attributes.containsKey("class")) {
            if (attributes["class"] == "invisible") {
                return ""
            }
        }

        // 改行の場合は素直に改行
        if (name.equals("br", ignoreCase = true)) {
            return "\n"
        }

        val builder: java.lang.StringBuilder = java.lang.StringBuilder()
        for (element in elements) {
            builder.append(element.toString())
        }

        return builder.toString()
    }

    fun getElements(): List<XmlElement> {
        return elements
    }

    fun setElements(elements: List<XmlElement>) {
        this.elements = elements
    } //endregion
}
