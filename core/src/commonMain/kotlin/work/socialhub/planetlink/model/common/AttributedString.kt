package work.socialhub.planetlink.model.common

import work.socialhub.planetlink.define.AttributedType.simple
import work.socialhub.planetlink.model.Emoji

/**
 * String With Attributes
 * 属性付き文字列
 */
class AttributedString {

    var elements: List<AttributedElement>

    /**
     * Make Attributes String with AttributedElements list.
     * 属性付き要素から属性付き文字列を生成
     */
    constructor(elements: List<AttributedElement>) {
        this.elements = elements
    }

    /**
     * Attributed String with plain text and element types.
     * 文字列から属性文字列を作成 (属性を指定)
     */
    private constructor(text: String, kinds: List<AttributedType>) {
        val model = AttributedItem()
        model.kind = AttributedKind.PLAIN
        model.displayText = text

        var stream = mutableListOf(model)

        for (kind in kinds) {
            stream = stream.map {
                scanElements(it, kind)
            }.flatten()
                .map { it as AttributedItem }
                .toMutableList()
        }

        elements = stream
    }

    /**
     * Add Emoji Element
     * 絵文字要素を追加
     */
    fun addEmojiElement(emojis: List<Emoji>?) {
        if (!emojis.isNullOrEmpty()) {

            var stream = mutableListOf<AttributedElement>()
            stream.addAll(elements)

            for (emoji in emojis) {
                stream = stream
                    .map { elem ->
                        scanEmojis(
                            elem,
                            emoji
                        )
                    }
                    .flatten()
                    .toMutableList()
            }
            elements = stream
                .map { it as AttributedItem }
                .toMutableList()
        }
    }


    /**
     * Get Display Text
     * 表示文字列を取得
     */
    val displayText: String
        get() = elements
            .map { it.displayText }
            .joinToString { "" }

    /**
     * Scan elements
     * エレメントを走査
     */
    private fun scanElements(
        element: AttributedElement,
        kind: AttributedType
    ): List<AttributedElement> {

        if (element.kind === AttributedKind.PLAIN) {
            val text: String = element.displayText

            if (text.isNotEmpty()) {

                // プレーン文字列の場合にスキャンして走査
                val regex = kind.regex
                val match = regex.find(text)

                // 見つかった場合分割
                if (match != null) {
                    val i = match.range.first
                    val found = match.value

                    if (i >= 0) {
                        val before = text.substring(0, i)
                        val after = text.substring(i + found.length)
                        val results = mutableListOf<AttributedElement>()

                        run {
                            val model = AttributedItem()
                            model.kind = AttributedKind.PLAIN
                            model.displayText = before
                            results.add(model)
                        }
                        run {
                            val model = AttributedItem()
                            model.displayText = kind.displayedText(match)
                            model.expandedText = kind.expandedText(match)
                            model.kind = kind.kind
                            results.add(model)
                        }
                        run {
                            val model = AttributedItem()
                            model.kind = AttributedKind.PLAIN
                            model.displayText = after

                            // 再帰的に作成したオブジェクトに対して走査
                            results.addAll(scanElements(model, kind))
                        }
                        return results

                    } else {
                        // 特殊環境下でエラーになるようなので調査のためログを挟む
                        println("UnExpected Status")
                        println("Text : $text")
                        println("Found: $found")
                        println("Index: $i")
                    }
                }
            }
        }
        return listOf(element)
    }

    /**
     * Scan emojis
     * 絵文字を抽出
     */
    private fun scanEmojis(
        element: AttributedElement,
        emoji: Emoji
    ): List<AttributedElement> {

        // 文字列の場合のみが対象
        if (element.kind === AttributedKind.PLAIN) {
            val text: String = element.displayText

            // プレーン文字列の場合にスキャンして走査
            val regex = ":${emoji.shortCode}:".toRegex()
            val match = regex.find(text)

            // 見つかった場合分割
            if (match != null) {
                val i = match.range.first
                val found = match.value

                if (i >= 0) {
                    val before = text.substring(0, i)
                    val after = text.substring(i + found.length)
                    val results = mutableListOf<AttributedElement>()

                    run {
                        val model = AttributedItem()
                        model.kind = AttributedKind.PLAIN
                        model.displayText = before
                        results.add(model)
                    }
                    run {
                        val model = AttributedItem()
                        model.displayText = regex.toString()
                        model.expandedText = emoji.imageUrl
                        model.kind = AttributedKind.EMOJI
                        results.add(model)
                    }
                    run {
                        val model = AttributedItem()
                        model.kind = AttributedKind.PLAIN
                        model.displayText = after

                        // 再帰的に作成したオブジェクトに対して走査
                        results.addAll(scanEmojis(model, emoji))
                    }
                    return results
                } else {
                    // 特殊環境下でエラーになるようなので調査のためログを挟む
                    println("UnExpected Status")
                    println("Text : $text")
                    println("Found: $found")
                    println("Index: $i")
                }
            }
        }

        return listOf(element)
    }

    companion object {

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
         * Make Attributes String with AttributedElements list.
         * 属性付き要素から属性付き文字列を生成
         */
        fun elements(elements: List<AttributedElement>): AttributedString {
            return AttributedString(elements)
        }
    }
}
