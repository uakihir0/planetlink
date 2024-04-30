package work.socialhub.planetlink.mastodon.expand

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import work.socialhub.planetlink.model.common.AttributedElement
import work.socialhub.planetlink.model.common.AttributedItem
import work.socialhub.planetlink.model.common.AttributedKind
import work.socialhub.planetlink.model.common.AttributedString

object AttributedStringEx {

    data class Tag(
        val name: String,
        val attributes: Map<String, String>,
        var text: String = "",
    ) {
        operator fun get(text: String): String? {
            return attributes[text]
        }
    }

    fun AttributedString.Companion.mastodon(
        text: String
    ): AttributedString {
        val elements = mutableListOf<AttributedElement>()
        val error: () -> Unit = {
            throw IllegalStateException(
                "Unexpected parse html."
            )
        }

        // 初期エレメントは div で作成
        val stack = ArrayDeque<Tag>()

        val handler = KsoupHtmlHandler.Builder()
            .onOpenTag { name, attributes, _ ->
                when (name) {

                    // <a>
                    "a" -> {
                        if (stack.isNotEmpty()) {
                            val last = stack.last()
                            if (last.text.isNotEmpty()) {
                                elements.add(AttributedItem().also {
                                    it.kind = AttributedKind.PLAIN
                                    it.displayText = last.text
                                    it.expandedText = last.text
                                })

                                // 空にして修正
                                last.text = ""
                            }
                        }

                        // タグの処理をスタックに追加
                        stack.add(Tag(name, attributes))
                    }

                    // <span>
                    "span" -> {
                        // タグの処理をスタックに追加
                        stack.add(Tag(name, attributes))
                    }

                    // <p>
                    "p" -> {
                        // タグの処理をスタックに追加
                        stack.add(Tag(name, attributes))
                    }
                }
            }
            .onCloseTag { name, _ ->
                when (name) {

                    // <a>
                    "a" -> {
                        val last = stack.removeLast()
                        if (last.name == "a") {
                            if (last["class"]?.contains("hashtag") == true) {
                                elements.add(AttributedItem().also {
                                    it.kind = AttributedKind.HASH_TAG
                                    it.displayText = last.text
                                    it.expandedText = last.text
                                })

                            } else if (last["class"]?.contains("u-url") == true) {
                                elements.add(AttributedItem().also {
                                    it.kind = AttributedKind.ACCOUNT
                                    it.displayText = last.text
                                    it.expandedText = last["href"]
                                })

                            } else {
                                elements.add(AttributedItem().also {
                                    it.kind = AttributedKind.LINK
                                    it.displayText = last.text
                                    it.expandedText = last["href"]
                                })
                            }
                        } else error()
                    }

                    // <br>
                    "br" -> {
                        if (stack.isNotEmpty()) {
                            stack.last().text += "\n"
                        }
                    }

                    // <span>
                    "span" -> {
                        val last = stack.removeLast()
                        if (last.name == "span") {
                            if (stack.isNotEmpty()) {
                                if (last["class"] != "invisible") {
                                    stack.last().text += last.text
                                }
                                if (last["class"] == "ellipsis") {
                                    stack.last().text += "..."
                                }

                            } else {
                                if (last["class"] != "invisible") {
                                    if (last["class"] == "ellipsis") {
                                        last.text += "..."
                                    }
                                    elements.add(AttributedItem().also {
                                        it.kind = AttributedKind.PLAIN
                                        it.displayText = last.text
                                        it.expandedText = last.text
                                    })
                                }
                            }
                        } else error()
                    }

                    // <p>
                    "p" -> {
                        val last = stack.removeLast()
                        if (last.name == "p") {
                            val t = if (last.text.isNotBlank())
                                "${last.text}\n\n" else ""

                            elements.add(AttributedItem().also {
                                it.kind = AttributedKind.PLAIN
                                it.displayText = t
                                it.expandedText = t
                            })
                        } else error()
                    }
                }
            }
            .onText { t ->
                if (stack.isNotEmpty()) {
                    stack.last().text += t
                }
                if (stack.isEmpty()) {
                    elements.add(AttributedItem().also {
                        it.kind = AttributedKind.PLAIN
                        it.displayText = t
                        it.expandedText = t
                    })
                }
            }
            .onEnd {
                if (elements.isNotEmpty()) {
                    val last = elements.last()
                    if (last is AttributedItem) {
                        if (last.kind == AttributedKind.PLAIN) {
                            last.displayText = last.displayText.trim()
                            last.expandedText = last.expandedText?.trim()
                        }
                    }
                }
            }
            .build()

        val parser = KsoupHtmlParser(handler)
        parser.write(text)
        parser.end()

        return AttributedString(elements)
    }
}