package net.socialhub.planetlink.model.common

import net.socialhub.planetlink.model.common.xml.XmlConvertRule

/**
 * String With Attributes
 * 属性付き文字列
 */
class AttributedString {
    private var elements: List<AttributedElement>

    // ============================================================================== //
    // Constructor
    // ============================================================================== //
    /**
     * Make Attributes String with AttributedElements list.
     * 属性付き要素から属性付き文字列を生成
     */
    private constructor(elements: List<AttributedElement>) {
        this.elements = elements
    }

    /**
     * Attributed String with plain text and element types.
     * 文字列から属性文字列を作成 (属性を指定)
     */
    private constructor(text: String, kinds: List<AttributedType>) {
        val model: AttributedItem = AttributedItem()
        model.setKind(AttributedKind.PLAIN)
        model.setDisplayText(text)

        var stream: java.util.stream.Stream<AttributedElement?> = java.util.stream.Stream.of<AttributedElement>(model)
        for (kind in kinds) {
            stream = stream
                .map<List<AttributedElement>>(java.util.function.Function<AttributedElement, List<AttributedElement>> { elem: AttributedElement ->
                    scanElements(
                        elem,
                        kind
                    )
                })
                .flatMap<AttributedElement>(java.util.function.Function<List<AttributedElement>, java.util.stream.Stream<out AttributedElement?>> { obj: Collection<*> -> obj.stream() })
        }
        elements = stream.collect(java.util.stream.Collectors.toList<Any>())
    }

    /**
     * Add Emoji Element
     * 絵文字要素を追加
     */
    fun addEmojiElement(emojis: List<Emoji?>?) {
        if (emojis != null && !emojis.isEmpty()) {
            var stream: java.util.stream.Stream<AttributedElement?> = elements.stream()
            for (emoji in emojis) {
                stream = stream
                    .map<List<AttributedElement>>(java.util.function.Function<AttributedElement, List<AttributedElement>> { elem: AttributedElement ->
                        scanEmojis(
                            elem,
                            emoji
                        )
                    })
                    .flatMap<AttributedElement>(java.util.function.Function<List<AttributedElement>, java.util.stream.Stream<out AttributedElement?>> { obj: Collection<*> -> obj.stream() })
            }
            elements = stream.collect(java.util.stream.Collectors.toList<Any>())
        }
    }

    /**
     * Get Elements
     * 要素情報の取得
     */
    fun getElements(): List<AttributedElement> {
        return this.elements
    }

    val displayText: String
        /**
         * Get Display Text
         * 表示文字列を取得
         */
        get() = elements.stream()
            .map<Any>(AttributedElement::getDisplayText)
            .collect(java.util.stream.Collectors.joining())

    /**
     * Scan elements
     * エレメントを走査
     */
    private fun scanElements(
        element: AttributedElement,
        kind: AttributedType
    ): List<AttributedElement?> {
        if (element.getKind() === AttributedKind.PLAIN) {
            val text: String = element.getDisplayText()
            if (!text.isEmpty()) {
                // プレーン文字列の場合にスキャンして走査

                val p: java.util.regex.Pattern = java.util.regex.Pattern.compile(kind.getRegex())
                val m: java.util.regex.Matcher = p.matcher(text)

                // 見つかった場合分割
                if (m.find()) {
                    val i: Int = m.start()
                    val found: String = m.group()

                    if (i >= 0) {
                        val before = text.substring(0, i)
                        val after = text.substring(i + found.length)
                        val results: MutableList<AttributedElement?> = java.util.ArrayList<AttributedElement>()

                        run {
                            val model: AttributedItem = AttributedItem()
                            model.setKind(AttributedKind.PLAIN)
                            model.setDisplayText(before)
                            results.add(model)
                        }
                        run {
                            val model: AttributedItem = AttributedItem()
                            model.setDisplayText(kind.getDisplayedText(m))
                            model.setExpandedText(kind.getExpandedText(m))
                            model.setKind(kind.getKind())
                            results.add(model)
                        }
                        run {
                            val model: AttributedItem = AttributedItem()
                            model.setKind(AttributedKind.PLAIN)
                            model.setDisplayText(after)

                            // 再帰的に作成したオブジェクトに対して走査
                            results.addAll(scanElements(model, kind))
                        }
                        return results
                    } else {
                        // 特殊環境下でエラーになるようなので調査のためログを挟む

                        val log: Logger = Logger.getLogger(AttributedString::class.java)
                        log.debug("UnExpected Status")
                        log.debug("Text : $text")
                        log.debug("Found: $found")
                        log.debug("Index: $i")
                    }
                }
            }
        }
        return listOf<AttributedElement>(element)
    }

    /**
     * Scan emojis
     * 絵文字を抽出
     */
    private fun scanEmojis(
        element: AttributedElement,
        emoji: Emoji
    ): List<AttributedElement?> {
        // 文字列の場合のみが対象

        if (element.getKind() === AttributedKind.PLAIN) {
            val text: String = element.getDisplayText()

            // プレーン文字列の場合にスキャンして走査
            val regex = ":" + emoji.shortCode + ":"
            val p: java.util.regex.Pattern = java.util.regex.Pattern.compile(regex)
            val m: java.util.regex.Matcher = p.matcher(text)

            // 見つかった場合分割
            if (m.find()) {
                val i: Int = m.start()
                val found: String = m.group()

                if (i >= 0) {
                    val before = text.substring(0, i)
                    val after = text.substring(i + found.length)
                    val results: MutableList<AttributedElement?> = java.util.ArrayList<AttributedElement>()

                    run {
                        val model: AttributedItem = AttributedItem()
                        model.setKind(AttributedKind.PLAIN)
                        model.setDisplayText(before)
                        results.add(model)
                    }
                    run {
                        val model: AttributedItem = AttributedItem()
                        model.setDisplayText(regex)
                        model.setExpandedText(emoji.imageUrl)
                        model.setKind(AttributedKind.EMOJI)
                        results.add(model)
                    }
                    run {
                        val model: AttributedItem = AttributedItem()
                        model.setKind(AttributedKind.PLAIN)
                        model.setDisplayText(after)

                        // 再帰的に作成したオブジェクトに対して走査
                        results.addAll(scanEmojis(model, emoji))
                    }
                    return results
                } else {
                    // 特殊環境下でエラーになるようなので調査のためログを挟む

                    val log: Logger = Logger.getLogger(AttributedString::class.java)
                    log.debug("UnExpected Status")
                    log.debug("Text : $text")
                    log.debug("Found: $found")
                    log.debug("Index: $i")
                }
            }
        }

        return listOf<AttributedElement>(element)
    }

    companion object {
        // ============================================================================== //
        // Static functions
        // ============================================================================== //
        /**
         * Make Attributed String from plain text.
         * 装飾無しテキストから属性付き文字列を作成
         */
        fun plain(string: String?): AttributedString {
            return AttributedString(if ((string != null)) string else "", simple())
        }

        /**
         * Make Attributed String from plain text with kinds.
         * 装飾無しテキストから属性付き文字列を作成 (種類を指定)
         */
        fun plain(string: String?, kinds: List<AttributedType>): AttributedString {
            return AttributedString(if ((string != null)) string else "", kinds)
        }

        /**
         * Make Attributed String from XHTML text.
         * XHTML テキストから属性付き文字列を作成
         */
        fun xhtml(string: String?): AttributedString {
            return xhtml(string, XmlConvertRule())
        }

        /**
         * Make Attributed String from XHTML text with rule.
         * XHTML テキストから属性付き文字列を作成 (ルールを指定)
         */
        fun xhtml(string: String?, rule: XmlConvertRule?): AttributedString {
            return XmlParseUtil.xhtml(string).toAttributedString(rule)
        }

        /**
         * Make Attributes String with AttributedElements list.
         * 属性付き要素から属性付き文字列を生成
         */
        fun elements(elements: List<AttributedElement>): AttributedString {
            return AttributedString(elements)
        }
    }
}
